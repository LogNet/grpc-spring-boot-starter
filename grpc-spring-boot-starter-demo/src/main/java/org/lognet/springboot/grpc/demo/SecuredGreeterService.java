package org.lognet.springboot.grpc.demo;

import com.google.protobuf.Empty;
import io.grpc.examples.GreeterOuterClass;
import io.grpc.examples.SecuredGreeterGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Slf4j
@GRpcService(interceptors = { LogInterceptor.class })
@Secured("SCOPE_profile")
public class SecuredGreeterService extends SecuredGreeterGrpc.SecuredGreeterImplBase{

    @Override
    public void sayAuthHello(Empty request, StreamObserver<GreeterOuterClass.HelloReply> responseObserver) {
        final Authentication auth = GrpcSecurity.AUTHENTICATION_CONTEXT_KEY.get();
        String user = auth.getName();
        if(auth instanceof JwtAuthenticationToken){
            user = JwtAuthenticationToken.class.cast(auth).getTokenAttributes().get("preferred_username").toString();
        }

        responseObserver.onNext(GreeterOuterClass.HelloReply
                .newBuilder()
                .setMessage(user)
                .build());
        responseObserver.onCompleted();
    }
}