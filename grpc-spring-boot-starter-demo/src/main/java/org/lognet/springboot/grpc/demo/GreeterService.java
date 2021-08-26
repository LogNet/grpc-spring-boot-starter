package org.lognet.springboot.grpc.demo;

import com.google.protobuf.Empty;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.Assert;

@Slf4j
@GRpcService(interceptors = { LogInterceptor.class })
public class GreeterService extends GreeterGrpc.GreeterImplBase {
    @Override
    public void sayHello(GreeterOuterClass.HelloRequest request, StreamObserver<GreeterOuterClass.HelloReply> responseObserver) {
        String message = "Hello " + request.getName();
        final GreeterOuterClass.HelloReply.Builder replyBuilder = GreeterOuterClass.HelloReply.newBuilder().setMessage(message);
        responseObserver.onNext(replyBuilder.build());
        responseObserver.onCompleted();
        log.info("Returning " +message);
    }

    @Override
    @Secured("SCOPE_profile")
    public void sayAuthHello(Empty request, StreamObserver<GreeterOuterClass.HelloReply> responseObserver) {


        final Authentication auth = GrpcSecurity.AUTHENTICATION_CONTEXT_KEY.get();
        Assert.isTrue(SecurityContextHolder.getContext().getAuthentication() == auth,()->"Authentication object should be the same as in GRPC context");
        if(null!=auth) {

            String user = auth.getName();
            if (auth instanceof JwtAuthenticationToken) {
                user = JwtAuthenticationToken.class.cast(auth).getTokenAttributes().get("preferred_username").toString();
            }
            responseObserver.onNext(GreeterOuterClass.HelloReply.newBuilder().setMessage(user).build());
        }else{
            responseObserver.onNext(GreeterOuterClass.HelloReply.newBuilder().setMessage("Hello").build());
        }
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("#person.getAge()<12")
    @PostAuthorize("returnObject.getAge()>5")
    public void sayPreAuthHello(GreeterOuterClass.Person person, StreamObserver<GreeterOuterClass.Person> responseObserver) {

        responseObserver.onNext(person.toBuilder().setNickName("dummy").build());
        responseObserver.onCompleted();

    }

    @Override
    @Secured({})
    public void sayAuthOnlyHello(Empty request, StreamObserver<GreeterOuterClass.HelloReply> responseObserver) {


        sayAuthHello(request,responseObserver);
    }

    @Override
    public void helloPersonValidResponse(GreeterOuterClass.Person request, StreamObserver<GreeterOuterClass.Person> responseObserver) {
        responseObserver.onNext(GreeterOuterClass.Person.newBuilder(request)
                .setNickName(request.getName().toLowerCase())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void helloPersonInvalidResponse(GreeterOuterClass.Person request, StreamObserver<GreeterOuterClass.Person> responseObserver) {
        responseObserver.onNext(GreeterOuterClass.Person.newBuilder(request)
                .clearNickName()
                .build());
        responseObserver.onCompleted();
    }


}