package org.lognet.springboot.grpc;


import com.fasterxml.jackson.databind.node.ObjectNode;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.prometheus.PrometheusConfig;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.security.AuthCallCredentials;
import org.lognet.springboot.grpc.security.AuthHeader;
import org.lognet.springboot.grpc.security.EnableGrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


@SpringBootTest(classes = DemoApp.class
        ,properties = {
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
    static class TestCfg extends GrpcSecurityConfigurerAdapter {
        @Override
        public void configure(GrpcSecurity builder) throws Exception {

            builder.authorizeRequests()
                    .anyMethod().authenticated()
                    .and()
                    .authenticationProvider(new AuthenticationProvider() {
                        @Override
                        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                            throw  new BadCredentialsException("");
                        }

                        @Override
                        public boolean supports(Class<?> authentication) {
                            return true;
                        }
                    })
                    .userDetailsService(new InMemoryUserDetailsManager());
        }



    }

   // @Test
    public void validationShouldInvokedBeforeAuthTest() {
        final GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(super.getChannel());
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> {
            stub.helloPersonValidResponse(GreeterOuterClass.Person.newBuilder()
                    .setAge(49)// valid
                    .clearName()//invalid
                    .build());
        });

        assertThat(e.getStatus().getCode(), Matchers.is(Status.Code.INVALID_ARGUMENT));


    }

    @Override
    public void simpleGreeting() throws ExecutionException, InterruptedException {
        AuthCallCredentials callCredentials = new AuthCallCredentials(
                AuthHeader.builder().basic("user","pwd".getBytes())
        );

        final GreeterGrpc.GreeterBlockingStub greeterFutureStub = GreeterGrpc.newBlockingStub(selectedChanel);
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () ->
            greeterFutureStub
                    .withCallCredentials(callCredentials)
                .sayHello(GreeterOuterClass.HelloRequest.newBuilder().setName(name).build())
        );
        assertThat(e.getStatus().getCode(),Matchers.is(Status.Code.UNAUTHENTICATED));

        final Timer timer = registry.find("grpc.server.calls").timer();
        assertThat(timer,notNullValue(Timer.class));

        Awaitility
                .waitAtMost(Duration.ofMillis(registryConfig.step().toMillis() * 2))
                .until(timer::count,greaterThan(0L));

        assertThat(timer.max(TimeUnit.MILLISECONDS),greaterThan(0d));
        assertThat(timer.mean(TimeUnit.MILLISECONDS),greaterThan(0d));
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS),greaterThan(0d));



        final String resultTag = timer.getId().getTag("result");
        assertThat(resultTag,notNullValue());
        assertThat(resultTag,is(Status.UNAUTHENTICATED.getCode().name()));

    }









}
