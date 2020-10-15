package org.lognet.springboot.grpc.security;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.security.access.intercept.AbstractSecurityInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
public class SecurityInterceptor extends AbstractSecurityInterceptor implements ServerInterceptor, Ordered {


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



        final byte[] authorization = headers.get(Metadata.Key.of("Authorization"+Metadata.BINARY_HEADER_SUFFIX, Metadata.BINARY_BYTE_MARSHALLER));


        final Authentication authentication = null==authorization?null:
                schemeSelector.getAuthScheme(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(authorization)))
                .orElseThrow(()->new RuntimeException("Can't get authentication from authorization header"));





        try {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

             beforeInvocation(call.getMethodDescriptor());

            Context ctx = Context.current()
                    .withValue(GrpcSecurity.AUTHENTICATION_CONTEXT_KEY, SecurityContextHolder.getContext().getAuthentication());

            return  Contexts.interceptCall(ctx,call,headers,next);
        } catch (Exception e) {


            call.close(Status.UNAUTHENTICATED.withDescription(e.getMessage()), new Metadata());
            return new ServerCall.Listener<ReqT>() {
                // noop
            };
        }finally {
            SecurityContextHolder.getContext().setAuthentication(null);
        }



    }





}
