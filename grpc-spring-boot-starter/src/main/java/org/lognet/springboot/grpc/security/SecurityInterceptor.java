package org.lognet.springboot.grpc.security;

import io.grpc.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.FailureHandlingSupport;
import org.lognet.springboot.grpc.GRpcServicesRegistry;
import org.lognet.springboot.grpc.MessageBlockingServerCallListener;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.lognet.springboot.grpc.recovery.GRpcRuntimeExceptionWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.MethodInvocationResult;
import org.springframework.security.authorization.method.PostAuthorizeAuthorizationManager;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.util.SimpleMethodInvocation;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class SecurityInterceptor implements ServerInterceptor, Ordered {

    private static final Context.Key<GrpcMethodInvocation<?, ?>> METHOD_INVOCATION = Context.key("METHOD_INVOCATION");

    private final GrpcSecurityMetadataSource securityMetadataSource;

    private final AuthenticationSchemeSelector schemeSelector;

    private AuthenticationManager authenticationManager;

    private GRpcServerProperties.SecurityProperties.Auth authCfg;

    private FailureHandlingSupport failureHandlingSupport;

    private GRpcServicesRegistry registry;

    /** Pre-authorization manager for {@code @PreAuthorize} annotations. */
    private PreAuthorizeAuthorizationManager preAuthorizeManager;

    /** Post-authorization manager for {@code @PostAuthorize} annotations. */
    private PostAuthorizeAuthorizationManager postAuthorizeManager;

    /**
     * Used to determine whether a denied authorization result is due to
     * the user being unauthenticated (UNAUTHENTICATED) rather than lacking
     * a required authority (PERMISSION_DENIED).
     */
    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();


    static class GrpcMethodInvocation<ReqT, RespT> extends SimpleMethodInvocation {
        final private ServerCall<ReqT, RespT> call;
        final private Metadata headers;
        final private ServerCallHandler<ReqT, RespT> next;
        @Getter
        @Setter
        private Object[] arguments;

        public GrpcMethodInvocation(GRpcServicesRegistry.GrpcServiceMethod serviceMethod, ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
            super(serviceMethod.getService(), serviceMethod.getMethod());
            this.call = call;
            this.headers = headers;
            this.next = next;
        }

        @Override
        public Object proceed() {
            return next.startCall(call, headers);
        }

        ServerCall<ReqT, RespT> getCall() {
            return call;
        }
    }


    public SecurityInterceptor(GrpcSecurityMetadataSource securityMetadataSource,
                               AuthenticationSchemeSelector schemeSelector,
                               PreAuthorizeAuthorizationManager preAuthorizeManager,
                               PostAuthorizeAuthorizationManager postAuthorizeManager) {
        this.securityMetadataSource = securityMetadataSource;
        this.schemeSelector = schemeSelector;
        this.preAuthorizeManager = preAuthorizeManager;
        this.postAuthorizeManager = postAuthorizeManager;
    }


    @Autowired
    public void setGRpcServicesRegistry(GRpcServicesRegistry registry) {
        this.registry = registry;
    }

    @Autowired
    public void setFailureHandlingSupport(@Lazy FailureHandlingSupport failureHandlingSupport) {
        this.failureHandlingSupport = failureHandlingSupport;
    }

    public void setConfig(GRpcServerProperties.SecurityProperties.Auth authCfg) {
        this.authCfg = Optional.ofNullable(authCfg).orElseGet(GRpcServerProperties.SecurityProperties.Auth::new);
    }

    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public int getOrder() {
        return Optional.ofNullable(authCfg.getInterceptorOrder()).orElse(Ordered.HIGHEST_PRECEDENCE + 1);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        final CharSequence authorization = Optional.ofNullable(headers.get(Metadata.Key.of("Authorization" + Metadata.BINARY_HEADER_SUFFIX, Metadata.BINARY_BYTE_MARSHALLER)))
                .map(auth -> (CharSequence) StandardCharsets.UTF_8.decode(ByteBuffer.wrap(auth)))
                .orElse(headers.get(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)));

        try {
            final Context grpcSecurityContext;
            try {
                grpcSecurityContext = setupGRpcSecurityContext(call, headers, next, authorization);
            } catch (RuntimeException e) {
                return fail(next, call, headers, e);
            } catch (Exception e) {
                return fail(next, call, headers, new GRpcRuntimeExceptionWrapper(e));
            }
            return Contexts.interceptCall(grpcSecurityContext, call, headers, authenticationPropagatingHandler(next));
        } finally {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }

    private <ReqT, RespT> ServerCallHandler<ReqT, RespT> authenticationPropagatingHandler(ServerCallHandler<ReqT, RespT> next) {

        return (call, headers) -> new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(next.startCall(postAuthorizeInterceptingCall(call), headers)) {

            @Override
            public void onMessage(ReqT message) {
                propagateAuthentication(() -> {
                            try {
                                switch (call.getMethodDescriptor().getType()) {
                                    // server streaming and unary calls generated with 2 parameters,
                                    // first one is the actual input
                                    case SERVER_STREAMING:
                                    case UNARY:
                                        METHOD_INVOCATION.get().setArguments(new Object[]{message, null});
                                        break;
                                    // client  streaming and bidi streaming  calls generated with 1 parameter
                                    case BIDI_STREAMING:
                                    case CLIENT_STREAMING:
                                    case UNKNOWN:
                                        METHOD_INVOCATION.get().setArguments(new Object[]{message});
                                        break;
                                    default:
                                        log.error("Unsupported call type " + call.getMethodDescriptor().getType());
                                        throw new StatusRuntimeException(Status.UNAUTHENTICATED);
                                }

                                performPreAuthorization(METHOD_INVOCATION.get());
                                super.onMessage(message);
                            } catch (RuntimeException e) {
                                failureHandlingSupport.closeCall(e, call, headers);
                            } catch (Exception e) {
                                failureHandlingSupport.closeCall(new GRpcRuntimeExceptionWrapper(e), call, headers);
                            } finally {
                                METHOD_INVOCATION.get().setArguments(null);
                            }
                        }
                );
            }

            @Override
            public void onHalfClose() {
                propagateAuthentication(super::onHalfClose);
            }

            @Override
            public void onCancel() {
                propagateAuthentication(super::onCancel);
            }

            @Override
            public void onComplete() {
                propagateAuthentication(super::onComplete);
            }

            @Override
            public void onReady() {
                propagateAuthentication(super::onReady);
            }

            private void propagateAuthentication(Runnable runnable) {
                try {
                    SecurityContextHolder.getContext().setAuthentication(GrpcSecurity.AUTHENTICATION_CONTEXT_KEY.get());
                    runnable.run();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }

        };
    }

    /**
     * Wraps the call so that each outbound message is post-authorized before being sent.
     */
    private <RespT, ReqT> ServerCall<RespT, ReqT> postAuthorizeInterceptingCall(ServerCall<RespT, ReqT> call) {
        return new ForwardingServerCall.SimpleForwardingServerCall<RespT, ReqT>(call) {
            @Override
            public void sendMessage(ReqT message) {
                GrpcMethodInvocation<?, ?> mi = METHOD_INVOCATION.get();
                if (mi != null) {
                    MethodInvocationResult result = new MethodInvocationResult(mi, message);
                    Supplier<Authentication> authSupplier = () -> GrpcSecurity.AUTHENTICATION_CONTEXT_KEY.get();
                    checkPostAuthorize(authSupplier, result);
                }
                super.sendMessage(message);
            }
        };
    }

    /**
     * Builds the pre-authorization manager for the given method invocation.
     *
     * <p>Uses {@code anyOf} semantics (matching the old {@code AffirmativeBased} with
     * {@code allowIfAllAbstainDecisions=true}):
     * <ul>
     *   <li>Grant immediately if the gRPC-registry rule grants.</li>
     *   <li>Grant immediately if {@code @PreAuthorize} expression grants.</li>
     *   <li>Grant by default if ALL managers abstain (no rules apply).</li>
     *   <li>Deny only if at least one manager denies and none grants.</li>
     * </ul>
     *
     * <p>Note: {@code @Secured} annotations are handled by
     * {@link GrpcServiceAuthorizationConfigurer#processSecuredAnnotation()} at startup time
     * and added to the gRPC registry metadata manager — they are NOT checked separately here.
     *
     * @param mi the method invocation (arguments may be null at call-setup time)
     * @return the composite {@link AuthorizationManager}
     */
    @SuppressWarnings("unchecked")
    private AuthorizationManager<GrpcMethodInvocation<?, ?>> buildPreAuthManager(GrpcMethodInvocation<?, ?> mi) {
        // gRPC-registry-driven rule for this specific method (may be null if no rule is registered)
        AuthorizationManager<GrpcMethodInvocation<?, ?>> metaManager =
                securityMetadataSource.getAuthorizationManager(mi);

        // @PreAuthorize wrapper: abstains when arguments are null (to mirror old behaviour of
        // PreInvocationAuthorizationAdviceVoter that returned ACCESS_ABSTAIN when arguments==null)
        AuthorizationManager<GrpcMethodInvocation<?, ?>> preAuthWrapper = (authSupplier, inv) -> {
            if (inv.getArguments() == null) {
                // Arguments not yet available — abstain, to be re-evaluated in onMessage()
                return null;
            }
            try {
                return preAuthorizeManager.authorize(authSupplier, inv);
            } catch (AuthenticationException | AccessDeniedException e) {
                throw e;
            } catch (Exception e) {
                // Expression evaluation failure (e.g. missing argument binding) — abstain
                log.trace("@PreAuthorize expression evaluation failed, treating as abstain", e);
                return null;
            }
        };

        // Combine with anyOf semantics:
        // - Grant if any grants (AffirmativeBased behavior)
        // - Default GRANT if all abstain (allowIfAllAbstainDecisions=true)
        if (metaManager != null) {
            return AuthorizationManagers.anyOf(
                    new AuthorizationDecision(true),
                    metaManager,
                    preAuthWrapper
            );
        } else {
            // No registry rule for this method; only @PreAuthorize matters.
            // Still use anyOf with default-grant so that methods with no rules at all are allowed.
            return AuthorizationManagers.anyOf(
                    new AuthorizationDecision(true),
                    preAuthWrapper
            );
        }
    }

    /**
     * Performs pre-authorization for the gRPC method invocation.
     * Called from {@code onMessage()} with actual arguments set.
     */
    private void performPreAuthorization(GrpcMethodInvocation<?, ?> mi) {
        Supplier<Authentication> authSupplier = () -> SecurityContextHolder.getContext().getAuthentication();
        AuthorizationManager<GrpcMethodInvocation<?, ?>> manager = buildPreAuthManager(mi);
        AuthorizationResult result = manager.authorize(authSupplier, mi);
        if (result != null && !result.isGranted()) {
            throwAccessDenied(authSupplier.get(), result);
        }
    }

    /**
     * Performs post-authorization. Covers {@code @PostAuthorize} annotations.
     */
    private void checkPostAuthorize(Supplier<Authentication> authSupplier, MethodInvocationResult result) {
        AuthorizationResult postResult = postAuthorizeManager.authorize(authSupplier, result);
        if (postResult != null && !postResult.isGranted()) {
            throwAccessDenied(authSupplier.get(), postResult);
        }
    }

    /**
     * Translates a denied authorization result into the appropriate exception:
     * <ul>
     *   <li>If the user is not authenticated (null or anonymous), throws
     *       {@link InsufficientAuthenticationException} → gRPC UNAUTHENTICATED status.</li>
     *   <li>Otherwise, throws {@link AuthorizationDeniedException} → gRPC PERMISSION_DENIED status.</li>
     * </ul>
     * This mirrors the behaviour of Spring Security's {@code ExceptionTranslationFilter}.
     */
    private void throwAccessDenied(Authentication authentication, AuthorizationResult result) {
        if (authentication == null || trustResolver.isAnonymous(authentication)) {
            throw new InsufficientAuthenticationException("Full authentication is required to access this resource");
        }
        throw new AuthorizationDeniedException("Access Denied", result);
    }

    private <RespT, ReqT> Context setupGRpcSecurityContext(ServerCall<RespT, ReqT> call, Metadata headers,
                                                           ServerCallHandler<RespT, ReqT> next, CharSequence authorization) {
        final Authentication authentication = null == authorization ? null :
                schemeSelector.getAuthScheme(authorization)
                        .orElseThrow(() -> new StatusRuntimeException(Status.UNAUTHENTICATED));

        // If we have a pre-authentication token, run it through the AuthenticationManager to get
        // a fully authenticated token (with granted authorities).
        final Authentication authenticatedAuth;
        if (authentication != null && authenticationManager != null && !authentication.isAuthenticated()) {
            authenticatedAuth = authenticationManager.authenticate(authentication);
        } else {
            authenticatedAuth = authentication;
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticatedAuth);
        SecurityContextHolder.setContext(context);

        final GRpcServicesRegistry.GrpcServiceMethod grpcServiceMethod = registry.getGrpServiceMethod(call.getMethodDescriptor());
        final GrpcMethodInvocation<RespT, ReqT> methodInvocation = new GrpcMethodInvocation<>(grpcServiceMethod, call, headers, next);

        // Perform the initial authorization check at call setup time.
        // At this point, arguments are null, so @PreAuthorize with argument-dependent expressions
        // will abstain (same behaviour as the old PreInvocationAuthorizationAdviceVoter).
        // The full @PreAuthorize check (with arguments) happens in onMessage().
        performInitialAuthorizationCheck(methodInvocation);

        return Context.current()
                .withValue(GrpcSecurity.AUTHENTICATION_CONTEXT_KEY, SecurityContextHolder.getContext().getAuthentication())
                .withValue(METHOD_INVOCATION, methodInvocation);
    }

    /**
     * Performs the initial authorization check at call setup time (before any message has arrived).
     * Uses the same composite manager as {@link #performPreAuthorization}, but at this point the
     * method arguments are null so argument-dependent {@code @PreAuthorize} expressions will abstain.
     */
    private void performInitialAuthorizationCheck(GrpcMethodInvocation<?, ?> mi) {
        Supplier<Authentication> authSupplier = () -> SecurityContextHolder.getContext().getAuthentication();
        AuthorizationManager<GrpcMethodInvocation<?, ?>> manager = buildPreAuthManager(mi);
        AuthorizationResult result = manager.authorize(authSupplier, mi);
        if (result != null && !result.isGranted()) {
            throwAccessDenied(authSupplier.get(), result);
        }
    }

    private <RespT, ReqT> ServerCall.Listener<ReqT> fail(ServerCallHandler<ReqT, RespT> next, ServerCall<ReqT, RespT> call, Metadata headers, RuntimeException exception) throws RuntimeException {

        if (authCfg.isFailFast()) {
            failureHandlingSupport.closeCall(exception, call, headers);

            return new ServerCall.Listener<ReqT>() {
            };
        } else {
            return new MessageBlockingServerCallListener<ReqT>(next.startCall(call, headers)) {
                @Override
                public void onMessage(ReqT message) {
                    blockMessage();
                    failureHandlingSupport.closeCall(exception, call, headers, b -> b.request(message));
                }
            };
        }
    }

}
