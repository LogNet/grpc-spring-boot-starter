package org.lognet.springboot.grpc.demo;


import io.grpc.examples.CalculatorGrpc;
import io.grpc.examples.CalculatorOuterClass;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import io.grpc.stub.StreamObserver;

/**
 * Created by alexf on 28-Jan-16.
 */


@SpringBootApplication
public class DemoApp {

    @GRpcService(interceptors = { LogInterceptor.class })
    public static class GreeterService extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(GreeterOuterClass.HelloRequest request, StreamObserver<GreeterOuterClass.HelloReply> responseObserver) {
            final GreeterOuterClass.HelloReply.Builder replyBuilder = GreeterOuterClass.HelloReply.newBuilder().setMessage("Hello " + request.getName());
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }
    }

    @GRpcService
    public static class CalculatorService extends CalculatorGrpc.CalculatorImplBase{
        @Override
        public void calculate(CalculatorOuterClass.CalculatorRequest request, StreamObserver<CalculatorOuterClass.CalculatorResponse> responseObserver) {
            CalculatorOuterClass.CalculatorResponse.Builder resultBuilder = CalculatorOuterClass.CalculatorResponse.newBuilder();
            switch (request.getOperation()){
                case ADD:
                    resultBuilder.setResult(request.getNumber1()+request.getNumber2());
                    break;
                case SUBTRACT:
                    resultBuilder.setResult(request.getNumber1()-request.getNumber2());
                    break;
                case MULTIPLY:
                    resultBuilder.setResult(request.getNumber1()*request.getNumber2());
                    break;
                case DIVIDE:
                    resultBuilder.setResult(request.getNumber1()/request.getNumber2());
                    break;
                case UNRECOGNIZED:
                    break;
            }
            responseObserver.onNext(resultBuilder.build());
            responseObserver.onCompleted();


        }


    }

    public static void main(String[] args) {
        SpringApplication.run(DemoApp.class,args);
    }

}
