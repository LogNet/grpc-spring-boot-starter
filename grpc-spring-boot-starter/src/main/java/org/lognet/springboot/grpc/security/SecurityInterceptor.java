package org.lognet.springboot.grpc.security;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.security.access.intercept.AbstractSecurityInterceptor;
import org.springframework.security.access.intercept.InterceptorStatusToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.BearerTokenError;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.security.oauth2.server.resource.BearerTokenErrors.invalidToken;

@Slf4j
public class SecurityInterceptor extends AbstractSecurityInterceptor implements ServerInterceptor, Ordered {

    class SecurityServerCallListener<ReqT, RespT> extends ServerCall.Listener<ReqT> {
        private  InterceptorStatusToken token = null;
        private  Optional<ServerCall.Listener<ReqT>> listener;

        protected SecurityServerCallListener(ServerCall<ReqT, RespT> call,
                                             Metadata headers,
                                             ServerCallHandler<ReqT, RespT> next) {
            try {
                token = SecurityInterceptor.this.beforeInvocation(call.getMethodDescriptor());
                listener = Optional.of(next.startCall(call, headers));
            }catch (Exception e){
                call.close(Status.UNAUTHENTICATED.withDescription(e.getMessage()),new Metadata());
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
            listener.ifPresent(l->l.onMessage(message));

        }

        @Override
        public void onReady() {
            listener.ifPresent(ServerCall.Listener::onReady);
        }

        private void tearDown(){
            SecurityInterceptor.this.finallyInvocation(token);
            SecurityInterceptor.this.afterInvocation(token,null);
        }
    }



    private static final Pattern authorizationPattern = Pattern.compile(
            "^Bearer (?<token>[a-zA-Z0-9-._~+/]+)=*$",
            Pattern.CASE_INSENSITIVE);

    private GrpcSecurityMetadataSource securedMethods;

    public SecurityInterceptor(GrpcSecurityMetadataSource securedMethods) {
        this.securedMethods = securedMethods;
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

        Matcher matcher = authorizationPattern.matcher(authorization);

        if (!matcher.matches()) {
            BearerTokenError error = invalidToken("Bearer token is malformed");
            throw new OAuth2AuthenticationException(error);
        }

        String token = matcher.group("token");


        BearerTokenAuthenticationToken authenticationRequest = new BearerTokenAuthenticationToken(token);




            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authenticationRequest);
            SecurityContextHolder.setContext(context);



            return new SecurityServerCallListener<>(call, headers, next) ;




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
