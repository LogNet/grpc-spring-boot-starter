package org.lognet.springboot.grpc;

import io.grpc.ManagedChannelBuilder;
import io.grpc.ServerBuilder;
import io.grpc.examples.GreeterGrpc;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.context.GRpcServerInitializedEvent;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.validation.Valid;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
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
public class PureNettyTransportConfigTest extends  GrpcServerTestBase{



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

    @Override
    protected void setupTransportSecurity(ManagedChannelBuilder<?> channelBuilder, Resource certChain) throws IOException {
        SslContext sslContext = GrpcSslContexts.forClient().trustManager(certChain.getInputStream()).build();
        ((NettyChannelBuilder)channelBuilder)
                .useTransportSecurity()
                .sslContext(sslContext);
    }

    @BeforeClass
    public static void before()  {
        assertThrows(ClassNotFoundException.class,()->Class.forName("io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder"));
    }


    @Override
    protected GreeterGrpc.GreeterFutureStub beforeGreeting(GreeterGrpc.GreeterFutureStub stub) {
        final ArgumentCaptor<ServerBuilder> captor = ArgumentCaptor.forClass(ServerBuilder.class);
        Mockito.verify(configurer,Mockito.times(1)).configure(captor.capture());
        assertThat("Should be pure NettyServerBuilder, not repackaged",io.grpc.netty.NettyServerBuilder.class.isInstance(captor.getValue()));


        return super.beforeGreeting(stub);
    }


}
