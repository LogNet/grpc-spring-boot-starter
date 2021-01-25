package org.lognet.springboot.grpc.demo;

import io.grpc.examples.CalculatorGrpc;
import io.grpc.examples.CalculatorOuterClass;
import io.grpc.examples.SecuredCalculatorGrpc;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.annotation.Secured;

@Configuration
public class DemoAppConfiguration {


    @GRpcService(interceptors = NotSpringBeanInterceptor.class)
    public static class CalculatorService extends CalculatorGrpc.CalculatorImplBase{
        @Override
        public void calculate(CalculatorOuterClass.CalculatorRequest request, StreamObserver<CalculatorOuterClass.CalculatorResponse> responseObserver) {
            responseObserver.onNext(calculate(request));
            responseObserver.onCompleted();


        }
        static CalculatorOuterClass.CalculatorResponse calculate(CalculatorOuterClass.CalculatorRequest request){
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
            return  resultBuilder.build();
        }


    }

    @GRpcService(interceptors = NotSpringBeanInterceptor.class)
    @Secured({})
    public static class SecuredCalculatorService extends SecuredCalculatorGrpc.SecuredCalculatorImplBase{
        @Override
        public void calculate(CalculatorOuterClass.CalculatorRequest request, StreamObserver<CalculatorOuterClass.CalculatorResponse> responseObserver) {
            responseObserver.onNext(CalculatorService.calculate(request));
            responseObserver.onCompleted();


        }


    }
}
