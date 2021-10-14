package org.lognet.springboot.grpc.security;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.FailureHandlingSupport;
import org.lognet.springboot.grpc.MessageBlockingServerCallListener;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.intercept.AbstractSecurityInterceptor;
import org.springframework.security.access.intercept.InterceptorStatusToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
public class SecurityInterceptor extends AbstractSecurityInterceptor implements ServerInterceptor, Ordered {

    private static final Context.Key<InterceptorStatusToken> INTERCEPTOR_STATUS_TOKEN = Context.key("INTERCEPTOR_STATUS_TOKEN");

    private final GrpcSecurityMetadataSource securedMethods;

    private final AuthenticationSchemeSelector schemeSelector;

    private GRpcServerProperties.SecurityProperties.Auth authCfg;

    private FailureHandlingSupport failureHandlingSupport;


    public SecurityInterceptor(GrpcSecurityMetadataSource securedMethods,AuthenticationSchemeSelector schemeSelector) {
        this.securedMethods = securedMethods;
        this.schemeSelector = schemeSelector;
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
        return MethodDescriptor.class;
    }

    @Override
    public GrpcSecurityMetadataSource obtainSecurityMetadataSource() {
        return securedMethods;
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
                grpcSecurityContext = setupGRpcSecurityContext(call, authorization);
            } catch (AccessDeniedException | AuthenticationException e) {
                return fail(next, call, headers, e);
            } catch (Exception e) {
                return fail(next, call, headers, new AuthenticationException("Authentication failure.", e) {
                });
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
                propagateAuthentication(() -> super.onMessage(message));
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

    private Context setupGRpcSecurityContext(ServerCall<?, ?> call, CharSequence authorization) {
        final Authentication authentication = null == authorization ? null :
                schemeSelector.getAuthScheme(authorization)
                        .orElseThrow(() -> new RuntimeException("Can't get authentication from authorization header"));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        final InterceptorStatusToken interceptorStatusToken = beforeInvocation(call.getMethodDescriptor());

        return Context.current()
                .withValues(GrpcSecurity.AUTHENTICATION_CONTEXT_KEY, SecurityContextHolder.getContext().getAuthentication(),
                        INTERCEPTOR_STATUS_TOKEN, interceptorStatusToken);
    }

    private <RespT, ReqT> ServerCall.Listener<ReqT> fail(ServerCallHandler<ReqT, RespT> next, ServerCall<ReqT, RespT> call, Metadata headers, RuntimeException exception) throws RuntimeException {

        if (authCfg.isFailFast()) {
            failureHandlingSupport.closeCall(exception, call, headers, b -> {

            });

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
