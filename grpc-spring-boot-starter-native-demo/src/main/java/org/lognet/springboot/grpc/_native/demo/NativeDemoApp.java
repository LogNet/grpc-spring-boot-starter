package org.lognet.springboot.grpc._native.demo;

import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;


@SpringBootApplication
public class NativeDemoApp {

    @GRpcService
    static class HelloService extends GreeterGrpc.GreeterImplBase{
        @Override
        public void sayHello(GreeterOuterClass.HelloRequest request, StreamObserver<GreeterOuterClass.HelloReply> responseObserver) {
            responseObserver.onNext(GreeterOuterClass.HelloReply.newBuilder()
                    .setMessage(String.format("Hello, %s", request.getName()))
                    .build());
            responseObserver.onCompleted();
        }
    }

    public static void main(String[] args) {
        final ConfigurableApplicationContext applicationContext = SpringApplication.run(NativeDemoApp.class, args);
        final Boolean autoStop = applicationContext.getEnvironment().getProperty("auto-stop", Boolean.class, false);
        if (autoStop) {
            applicationContext.close();
        }
    }
}

