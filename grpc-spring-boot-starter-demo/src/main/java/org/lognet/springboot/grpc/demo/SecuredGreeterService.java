package org.lognet.springboot.grpc.demo;

import com.google.protobuf.Empty;
import io.grpc.examples.GreeterOuterClass;
import io.grpc.examples.SecuredGreeterGrpc;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.Assert;

import java.util.Optional;


@GRpcService(interceptors = { LogInterceptor.class })
@Secured("SCOPE_profile")
public class SecuredGreeterService extends SecuredGreeterGrpc.SecuredGreeterImplBase{

    @Override
    public void sayAuthHello(Empty request, StreamObserver<GreeterOuterClass.HelloReply> responseObserver) {
        final Authentication auth = GrpcSecurity.AUTHENTICATION_CONTEXT_KEY.get();
        Assert.isTrue(SecurityContextHolder.getContext().getAuthentication() == auth,()->"Authentication object should be the same as in GRPC context");
        String user = auth.getName();
        if(auth instanceof JwtAuthenticationToken){
            user = JwtAuthenticationToken.class.cast(auth).getTokenAttributes().get("preferred_username").toString();
        }

        reply(user,responseObserver);
    }



    @Override
    public void sayAuthHello2(Empty request, StreamObserver<GreeterOuterClass.HelloReply> responseObserver) {
        final Optional<Authentication> authentication = Optional.ofNullable(GrpcSecurity.AUTHENTICATION_CONTEXT_KEY.get());


        Assert.isTrue(SecurityContextHolder.getContext().getAuthentication() == authentication.orElse(null),()->"Authentication object should be the same as in GRPC context");

        String userName = authentication
                .map(Authentication::getName)
                .orElse("anonymous");
        reply(userName,responseObserver);
    }

    private void reply(String userName,StreamObserver<GreeterOuterClass.HelloReply> responseObserver){
        responseObserver.onNext(GreeterOuterClass.HelloReply
                .newBuilder()
                .setMessage(userName)
                .build());
        responseObserver.onCompleted();
    }
}