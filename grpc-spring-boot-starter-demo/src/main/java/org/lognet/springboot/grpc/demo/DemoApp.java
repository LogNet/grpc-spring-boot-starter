package org.lognet.springboot.grpc.demo;


import io.grpc.examples.CalculatorGrpc;
import io.grpc.examples.CalculatorOuterClass;
<<<<<<< HEAD
=======
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

>>>>>>> upstream/master
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
public class DemoApp {

<<<<<<< HEAD
    @GRpcService(interceptors = {LogInterceptor.class}, grpcServiceOuterClass = GreeterGrpc.class, serviceName = "GreeterService", version = "0.0.8-SNAPSHOT")
    public static class GreeterService extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(GreeterOuterClass.HelloRequest request, StreamObserver<GreeterOuterClass.HelloReply> responseObserver) {
            final GreeterOuterClass.HelloReply.Builder replyBuilder = GreeterOuterClass.HelloReply.newBuilder().setMessage("Hello " + request.getName());
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }
    }
=======
    @Bean
    public GreeterService greeterService() {
		return new GreeterService();
	}
>>>>>>> upstream/master

    @GRpcService(grpcServiceOuterClass = CalculatorGrpc.class, serviceName = "CalculatorService", version = "0.0.8-SNAPSHOT")
    public static class CalculatorService extends CalculatorGrpc.CalculatorImplBase {
        @Override
        public void calculate(CalculatorOuterClass.CalculatorRequest request, StreamObserver<CalculatorOuterClass.CalculatorResponse> responseObserver) {
            CalculatorOuterClass.CalculatorResponse.Builder resultBuilder = CalculatorOuterClass.CalculatorResponse.newBuilder();
            switch (request.getOperation()) {
                case ADD:
                    resultBuilder.setResult(request.getNumber1() + request.getNumber2());
                    break;
                case SUBTRACT:
                    resultBuilder.setResult(request.getNumber1() - request.getNumber2());
                    break;
                case MULTIPLY:
                    resultBuilder.setResult(request.getNumber1() * request.getNumber2());
                    break;
                case DIVIDE:
                    resultBuilder.setResult(request.getNumber1() / request.getNumber2());
                    break;
                case UNRECOGNIZED:
                    break;
            }
            responseObserver.onNext(resultBuilder.build());
            responseObserver.onCompleted();


        }


    }

    public static void main(String[] args) {
        SpringApplication.run(DemoApp.class, args);
    }

}
