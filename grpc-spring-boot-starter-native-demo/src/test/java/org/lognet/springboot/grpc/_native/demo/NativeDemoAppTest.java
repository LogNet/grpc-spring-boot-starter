package org.lognet.springboot.grpc._native.demo;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class NativeDemoAppTest {

    @Container
    private GenericContainer<?> grpcDemoAppContainer = new GenericContainer<>(
                DockerImageName.parse(System.getProperty("image-name"))
            ).withExposedPorts(6565)
            .waitingFor(new LogMessageWaitStrategy()
                    .withRegEx(".*listening on port 6565.*")
                    )
            ;
    @Test
    void nativeImageTest() {
        int grpcPort = grpcDemoAppContainer.getFirstMappedPort();
        assertTrue(1<grpcPort);
        final ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", grpcPort)
                .usePlaintext()
                .build();
        String name = "Johnny";
        final GreeterOuterClass.HelloReply reply = GreeterGrpc.newBlockingStub(channel).sayHello(GreeterOuterClass.HelloRequest.newBuilder()
                .setName(name)
                .build());
        assertEquals(reply.getMessage(),String.format("Hello, %s",name));



    }
}