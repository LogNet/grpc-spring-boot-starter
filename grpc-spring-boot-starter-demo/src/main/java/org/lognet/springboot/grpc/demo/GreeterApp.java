package org.lognet.springboot.grpc.demo;


import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by alexf on 28-Jan-16.
 */


@SpringBootApplication
public class GreeterApp {

    @GRpcService(grpcClass = GreeterGrpc.class)
    public static class GreeterService implements GreeterGrpc.Greeter{
        @Override
        public void sayHello(GreeterOuterClass.HelloRequest request, StreamObserver<GreeterOuterClass.HelloReply> responseObserver) {
            System.out.println("Got request");
            responseObserver.onNext(GreeterOuterClass.HelloReply.newBuilder().setMessage("Hello "+request.getName()).build());
            responseObserver.onCompleted();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(GreeterApp.class,args);
    }
}

