package org.lognet.springboot.grpc.health;

import io.grpc.examples.GreeterGrpc;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class, CustomGRpcHealthStatusManagerTest.Cfg.class}, webEnvironment = NONE)
@ActiveProfiles("disable-security")
public class CustomGRpcHealthStatusManagerTest extends GrpcServerTestBase {
    @TestConfiguration
    static class Cfg{
        static class MyCustomHealthStatusManager extends DefaultHealthStatusManager{}
        @Bean
        public GRpcHealthStatusManager myCustom(){
            return new MyCustomHealthStatusManager();
        }
    }

    @Autowired
    private  GRpcHealthStatusManager healthStatusManager;

    @Test
    public void contextLoads() {
        assertThat(healthStatusManager,isA(Cfg.MyCustomHealthStatusManager.class));
    }

    @Test
    public void testHealthCheck() throws ExecutionException, InterruptedException {
        final HealthCheckRequest healthCheckRequest = HealthCheckRequest.newBuilder().setService(GreeterGrpc.getServiceDescriptor().getName()).build();
        final HealthGrpc.HealthFutureStub healthFutureStub = HealthGrpc.newFutureStub(channel);
        final HealthCheckResponse.ServingStatus servingStatus = healthFutureStub.check(healthCheckRequest).get().getStatus();

        assertThat(servingStatus, is(HealthCheckResponse.ServingStatus.SERVING));
    }
}
