package org.lognet.springboot.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.context.GRpcServerInitializedEvent;
import org.lognet.springboot.grpc.context.LocalRunningGrpcPort;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;


import static org.junit.Assert.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE,
        properties = {
                "grpc.netty-server.additional-listen-addresses[0]=localhost:0",
                "grpc.netty-server.primary-listen-address=localhost:0"

}
)
@Import(PureNettyTransportConfigTest.TestConfig.class)
public class PureNettyTransportConfigTest {

    @LocalRunningGrpcPort
    protected int runningPort;

    @Configuration
    static class TestConfig {

        @EventListener
        public void onServer(GRpcServerInitializedEvent e){
            final List<? extends SocketAddress> listenSockets = e.getServer().getListenSockets();
            assertThat(listenSockets.size(), Matchers.is(2));
        }

        @Bean
        public GRpcServerBuilderConfigurer customGrpcServerBuilderConfigurer() {
            return Mockito.mock(GRpcServerBuilderConfigurer.class);
        }

    }
    @Autowired
    private GRpcServerBuilderConfigurer configurer;


    @Test
    public void simpleGreeting() throws ExecutionException, InterruptedException {

        assertThrows(ClassNotFoundException.class,()->Class.forName("io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder"));
        final ArgumentCaptor<ServerBuilder> captor = ArgumentCaptor.forClass(ServerBuilder.class);
        Mockito.verify(configurer,Mockito.times(1)).configure(captor.capture());
        assertThat("Should be pure NettyServerBuilder, not repackaged",io.grpc.netty.NettyServerBuilder.class.isInstance(captor.getValue()));

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", runningPort)
                .usePlaintext()
                .build();


        String name = "John";
        final GreeterGrpc.GreeterFutureStub greeterFutureStub = GreeterGrpc.newFutureStub(channel);
        final GreeterOuterClass.HelloRequest helloRequest = GreeterOuterClass.HelloRequest.newBuilder().setName(name).build();
        final String reply = greeterFutureStub.sayHello(helloRequest).get().getMessage();
        assertNotNull("Reply should not be null", reply);
        assertThat(String.format("Reply should contain name '%s'", name), reply.contains(name));


    }

}
