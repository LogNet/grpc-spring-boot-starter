package org.lognet.springboot.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {DemoApp.class, FactoryBeanMixedWithRegularBeansTest.FactoryBeanMixedWithRegularBeansTestConfiguration.class},
        webEnvironment = NONE,
        properties = "grpc.port=7776"
)
public class FactoryBeanMixedWithRegularBeansTest extends GrpcServerTestBase {

    private static final AtomicInteger counter = new AtomicInteger(0);

    @Override
    protected void afterGreeting() {
        assertThat(counter.get()).isEqualTo(2);
    }

    @Configuration
    static class FactoryBeanMixedWithRegularBeansTestConfiguration {

        @GRpcGlobalInterceptor
        static class ComponentInterceptor implements ServerInterceptor {

            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    Metadata headers,
                    ServerCallHandler<ReqT, RespT> next
            ) {
                counter.incrementAndGet();
                return next.startCall(call, headers);
            }

        }

        static class FactoryBeanInterceptor implements ServerInterceptor {

            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    Metadata headers,
                    ServerCallHandler<ReqT, RespT> next
            ) {
                counter.incrementAndGet();
                return next.startCall(call, headers);
            }

        }

        @Bean
        @GRpcGlobalInterceptor
        FactoryBeanInterceptor factoryBeanInterceptor() {
            return new FactoryBeanInterceptor();
        }

    }

}
