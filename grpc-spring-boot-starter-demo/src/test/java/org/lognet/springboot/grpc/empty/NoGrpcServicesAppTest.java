package org.lognet.springboot.grpc.empty;

import io.grpc.BindableService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GRpcServerRunner;
import org.lognet.springboot.grpc.context.GRpcServerInitializedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NoGrpcServicesAppTest {
    @Configuration
    public static class EmptyConfig{

        @Bean
        public GrpcServerEventListener grpcServerEventListener(){
            return new GrpcServerEventListener();
        }

        public static class GrpcServerEventListener implements ApplicationListener<GRpcServerInitializedEvent> {
            @Override
            public void onApplicationEvent(GRpcServerInitializedEvent e) {
                throw new RuntimeException("Should not be called");
            }
        }

    }
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private EmptyConfig.GrpcServerEventListener listener;


    @Test
    public void contextLoads() {
        assertThat(listener,Matchers.notNullValue(EmptyConfig.GrpcServerEventListener.class));
        assertThat(applicationContext.getBeanNamesForType(GRpcServerRunner.class), Matchers.emptyArray());
        assertThat(applicationContext.getBeanNamesForType(BindableService.class), Matchers.emptyArray());
    }
}
