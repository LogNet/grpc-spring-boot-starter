package org.lognet.springboot.grpc.demo;


import org.lognet.springboot.grpc.GrpcService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import io.grpc.stub.StreamObserver;

/**
 * Created by alexf on 28-Jan-16.
 */


@SpringBootApplication
public class GreeterApp {

    @GrpcService
    public static class GreeterService extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(GreeterOuterClass.HelloRequest request, StreamObserver<GreeterOuterClass.HelloReply> responseObserver) {
            final GreeterOuterClass.HelloReply.Builder replyBuilder = GreeterOuterClass.HelloReply.newBuilder().setMessage("Hello " + request.getName());
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(GreeterApp.class,args);
    }

}

