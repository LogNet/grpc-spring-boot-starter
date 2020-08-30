package org.lognet.springboot.grpc.security;

import io.grpc.*;
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

        final Authentication authentication = schemeSelector.getAuthScheme(authorization)
                .orElseThrow(()->new RuntimeException("Can't get authentication from authorization header"));


        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);


        return new SecurityServerCallListener<>(call, headers, next);


    }


//    public void setServices(Collection<BindableService> services){
//
//        for (BindableService s : services) {
//            final Secured securedAnn = AnnotationUtils.findAnnotation(s.getClass(), Secured.class);
//            final ServerServiceDefinition serverServiceDefinition = s.bindService();
//            if (null != securedAnn) {
//                serverServiceDefinition.getMethods().forEach(m -> securedMethods.compute(m.getMethodDescriptor(), (k, v) -> {
//                            Set<String> roles = new HashSet<>(Arrays.asList(securedAnn.value()));
//                            if (null != v) {
//                                roles.addAll(v);
//                            }
//                            return roles;
//
//                        }
//                ));
//            }
//
//            serverServiceDefinition.getMethods().forEach(methodDefinition -> {
//                final Optional<Method> method = Stream.of(s.getClass().getMethods())
//
//                        .filter(m -> {
//                            final String methodName = methodDefinition.getMethodDescriptor().getFullMethodName().substring(methodDefinition.getMethodDescriptor().getServiceName().length()+1);
//                            return methodName.equalsIgnoreCase(m.getName());
//                        })
//                        .findFirst();
//
//                method.ifPresent(m -> {
//                    final Secured securedMethodAnn = AnnotationUtils.findAnnotation(m, Secured.class);
//                    if (null != securedMethodAnn) {
//                        securedMethods.compute(methodDefinition.getMethodDescriptor(), (k, v) ->
//                                new HashSet<>(Arrays.asList(securedMethodAnn.value()))
//                        );
//                    }
//                });
//
//            });
//        }
//
//    }


}
