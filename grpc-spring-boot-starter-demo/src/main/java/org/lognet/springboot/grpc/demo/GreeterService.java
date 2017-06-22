package org.lognet.springboot.grpc.demo;

import io.grpc.ServerInterceptor;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.GRpcServiceBuilder;

import java.util.Arrays;
import java.util.Collection;

@GRpcService(interceptors = { LogInterceptor.class })
public class GreeterService extends GreeterGrpc.GreeterImplBase implements GRpcServiceBuilder {

    @Override
    public Collection<ServerInterceptor> getServiceInterceptors() {
        return Arrays.asList(
                new LogInterceptor("dynamicServiceLogger")
        );
    }

    @Override
    public void sayHello(GreeterOuterClass.HelloRequest request, StreamObserver<GreeterOuterClass.HelloReply> responseObserver) {
        final GreeterOuterClass.HelloReply.Builder replyBuilder = GreeterOuterClass.HelloReply.newBuilder().setMessage("Hello " + request.getName());
        responseObserver.onNext(replyBuilder.build());
        responseObserver.onCompleted();
    }
}