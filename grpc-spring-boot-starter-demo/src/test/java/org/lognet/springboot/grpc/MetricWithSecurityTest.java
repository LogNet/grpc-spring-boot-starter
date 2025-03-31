package org.lognet.springboot.grpc;


import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.auth.FailedAuthGrpcSecurityConfig;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.security.AuthCallCredentials;
import org.lognet.springboot.grpc.security.AuthHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;


@SpringBootTest(classes = DemoApp.class
        , properties = {
        "grpc.security.auth.fail-fast=false", // give metric interceptor a chance to record failed authentication
        "grpc.security.auth.interceptor-order=4",
        "grpc.metrics.interceptor-order=2",
}
)
@RunWith(SpringRunner.class)
@Import({MetricWithSecurityTest.TestCfg.class})
@ActiveProfiles("measure")
public class MetricWithSecurityTest extends GrpcServerTestBase {

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private PrometheusConfig registryConfig;

    @TestConfiguration
    static class TestCfg {
        @Bean
        public FailedAuthGrpcSecurityConfig securityConfig() {
            return new FailedAuthGrpcSecurityConfig();
        }
    }

    @Override
    public void simpleGreeting() throws Exception {
        AuthCallCredentials callCredentials = new AuthCallCredentials(
                AuthHeader.builder().basic("user", "pwd".getBytes())
        );

        final GreeterGrpc.GreeterBlockingStub greeterFutureStub = GreeterGrpc.newBlockingStub(selectedChanel);
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () ->
                greeterFutureStub
                        .withCallCredentials(callCredentials)
                        .sayHello(GreeterOuterClass.HelloRequest.newBuilder().setName(name).build())
        );
        assertThat(e.getStatus().getCode(), Matchers.is(Status.Code.UNAUTHENTICATED));

        final Timer timer = registry.find("grpc.server.calls").timer();
        assertThat(timer, notNullValue(Timer.class));

        Awaitility
                .waitAtMost(Duration.ofMillis(registryConfig.step().toMillis() * 2))
                .until(timer::count, greaterThan(0L));

        assertThat(timer.max(TimeUnit.MILLISECONDS), greaterThan(0d));
        assertThat(timer.mean(TimeUnit.MILLISECONDS), greaterThan(0d));
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS), greaterThan(0d));


        final String resultTag = timer.getId().getTag("result");
        assertThat(resultTag, notNullValue());
        assertThat(resultTag, is(Status.UNAUTHENTICATED.getCode().name()));

    }


}
