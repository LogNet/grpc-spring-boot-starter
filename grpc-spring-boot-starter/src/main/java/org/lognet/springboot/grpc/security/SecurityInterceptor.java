package org.lognet.springboot.grpc.security;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.security.access.intercept.AbstractSecurityInterceptor;
import org.springframework.security.access.intercept.InterceptorStatusToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Slf4j
public class SecurityInterceptor extends AbstractSecurityInterceptor implements ServerInterceptor, Ordered {

    class SecurityServerCallListener<ReqT, RespT> extends ServerCall.Listener<ReqT> {
        private InterceptorStatusToken token = null;
        private Optional<ServerCall.Listener<ReqT>> listener;

        protected SecurityServerCallListener(ServerCall<ReqT, RespT> call,
                                             Metadata headers,
                                             ServerCallHandler<ReqT, RespT> next) {
            try {
                token = SecurityInterceptor.this.beforeInvocation(call.getMethodDescriptor());
                listener = Optional.of(next.startCall(call, headers));
            } catch (Exception e) {
                call.close(Status.UNAUTHENTICATED.withDescription(e.getMessage()), new Metadata());
                listener = Optional.empty();
            }

        }

        @Override
        public void onHalfClose() {
            listener.ifPresent(ServerCall.Listener::onHalfClose);
            tearDown();
        }

        @Override
        public void onCancel() {

            listener.ifPresent(ServerCall.Listener::onCancel);
            tearDown();
        }

        @Override
        public void onComplete() {
            listener.ifPresent(ServerCall.Listener::onComplete);
            tearDown();
        }

        @Override
        public void onMessage(ReqT message) {
            listener.ifPresent(l -> l.onMessage(message));

        }

        @Override
        public void onReady() {
            listener.ifPresent(ServerCall.Listener::onReady);
        }

        private void tearDown() {
            SecurityInterceptor.this.finallyInvocation(token);
            SecurityInterceptor.this.afterInvocation(token, null);
        }
    }


    private GrpcSecurityMetadataSource securedMethods;
    private AuthenticationSchemeSelector schemeSelector;

    public SecurityInterceptor(GrpcSecurityMetadataSource securedMethods, AuthenticationSchemeSelector schemeSelector) {
        this.securedMethods = securedMethods;
        this.schemeSelector = schemeSelector;
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
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
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        final String authorization = headers.get(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER));


        final Authentication authentication = null==authorization?null:
                schemeSelector.getAuthScheme(authorization)
                .orElseThrow(()->new RuntimeException("Can't get authentication from authorization header"));


        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);


        return new SecurityServerCallListener<>(call, headers, next);


    }





}
