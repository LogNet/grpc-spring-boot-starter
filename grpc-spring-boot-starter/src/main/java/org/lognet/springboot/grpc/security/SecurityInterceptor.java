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
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.access.intercept.AbstractSecurityInterceptor;
import org.springframework.security.access.intercept.InterceptorStatusToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.util.SimpleMethodInvocation;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
public class SecurityInterceptor extends AbstractSecurityInterceptor implements ServerInterceptor, Ordered {

    private static final Context.Key<InterceptorStatusToken> INTERCEPTOR_STATUS_TOKEN = Context.key("INTERCEPTOR_STATUS_TOKEN");
    private static final Context.Key<GrpcMethodInvocation<?, ?>> METHOD_INVOCATION = Context.key("METHOD_INVOCATION");

    private final SecurityMetadataSource securityMetadataSource;

    private final AuthenticationSchemeSelector schemeSelector;

    private GRpcServerProperties.SecurityProperties.Auth authCfg;

    private FailureHandlingSupport failureHandlingSupport;

    private GRpcServicesRegistry registry;


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


    public SecurityInterceptor(SecurityMetadataSource securityMetadataSource, AuthenticationSchemeSelector schemeSelector) {
        this.securityMetadataSource = securityMetadataSource;
        this.schemeSelector = schemeSelector;
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

    @Override
    public int getOrder() {
        return Optional.ofNullable(authCfg.getInterceptorOrder()).orElse(Ordered.HIGHEST_PRECEDENCE + 1);
    }

    @Override
    public Class<?> getSecureObjectClass() {
        return GrpcMethodInvocation.class;
    }

    @Override
    public SecurityMetadataSource obtainSecurityMetadataSource() {
        return securityMetadataSource;
    }

    @Override
    /**
     *   Execute the same interceptor flow as original   FilterSecurityInterceptor/MethodSecurityInterceptor
     *   {
     *    InterceptorStatusToken token = super.beforeInvocation(mi);
     * 		Object result;
     * 		try {
     * 			result = mi.proceed();
     *        }
     * 		finally {
     * 			super.finallyInvocation(token);
     *        }
     * 		return super.afterInvocation(token, result);
     *    }
     */
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

        return (call, headers) -> new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(next.startCall(afterInvocationPropagator(call), headers)) {

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
                                        throw new StatusRuntimeException(Status.UNAUTHENTICATED) ;
                                }

                                beforeInvocation(METHOD_INVOCATION.get());
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
                try {
                    propagateAuthentication(super::onHalfClose);
                } finally {
                    finallyInvocation(INTERCEPTOR_STATUS_TOKEN.get());
                }
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

    private <RespT, ReqT> ServerCall<RespT, ReqT> afterInvocationPropagator(ServerCall<RespT, ReqT> call) {
        return new ForwardingServerCall.SimpleForwardingServerCall<RespT, ReqT>(call) {
            @Override
            public void sendMessage(ReqT message) {
                super.sendMessage((ReqT) afterInvocation(INTERCEPTOR_STATUS_TOKEN.get(), message));
            }
        };
    }

    private <RespT, ReqT> Context setupGRpcSecurityContext(ServerCall<RespT, ReqT> call, Metadata headers,
                                                           ServerCallHandler<RespT, ReqT> next, CharSequence authorization) {
        final Authentication authentication = null == authorization ? null :
                schemeSelector.getAuthScheme(authorization)
                        .orElseThrow(() -> new StatusRuntimeException(Status.UNAUTHENTICATED));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        final GRpcServicesRegistry.GrpcServiceMethod grpcServiceMethod = registry.getGrpServiceMethod(call.getMethodDescriptor());

        final GrpcMethodInvocation<RespT, ReqT> methodInvocation = new GrpcMethodInvocation<>(grpcServiceMethod, call, headers, next);
        final InterceptorStatusToken interceptorStatusToken = beforeInvocation(methodInvocation);

        return Context.current()
                .withValue(GrpcSecurity.AUTHENTICATION_CONTEXT_KEY, SecurityContextHolder.getContext().getAuthentication())
                .withValue(INTERCEPTOR_STATUS_TOKEN, interceptorStatusToken)
                .withValue(METHOD_INVOCATION, methodInvocation);
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
