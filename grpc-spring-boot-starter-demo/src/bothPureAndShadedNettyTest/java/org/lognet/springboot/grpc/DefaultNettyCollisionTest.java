package org.lognet.springboot.grpc;

import io.grpc.ServerBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ClassUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE)
@Import(DefaultNettyCollisionTest.TestConfig.class)
public class DefaultNettyCollisionTest extends GrpcServerTestBase {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public GRpcServerBuilderConfigurer customConfigurer(){
            return Mockito.mock(GRpcServerBuilderConfigurer.class);
        }
    }

    @Autowired
    private GRpcServerBuilderConfigurer configurer;

    @Test
    public void contextLoads() {
        assertNettyBuilderClass(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class);

    }

    protected void assertNettyBuilderClass(Class<? extends ServerBuilder<?>> nettyServerBuilderClass){
        ClassLoader classLoader = getClass().getClassLoader();

        assertThat("PureNetty is on classpath",ClassUtils.isPresent(io.grpc.netty.NettyServerBuilder.class.getName(), classLoader));
        assertThat("ShadedNetty is on classpath",ClassUtils.isPresent(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class.getName(), classLoader));

        Mockito.verify(configurer,Mockito.times(1))
                .configure(Mockito.any(nettyServerBuilderClass));
    }
}
