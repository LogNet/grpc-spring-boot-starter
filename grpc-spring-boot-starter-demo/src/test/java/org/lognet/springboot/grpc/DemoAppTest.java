package org.lognet.springboot.grpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.grpc.ServerInterceptor;
import io.grpc.examples.CalculatorGrpc;
import io.grpc.examples.CalculatorOuterClass;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.reflection.v1alpha.ServiceResponse;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by alexf on 28-Jan-16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class, TestConfig.class}, webEnvironment = RANDOM_PORT
        , properties = {"grpc.enableReflection=true",
        "grpc.shutdownGrace=-1",
        "spring.main.web-application-type=servlet"
})
@ActiveProfiles({"disable-security", "measure"})

public class DemoAppTest extends GrpcServerTestBase {

    @Autowired
    private PrometheusConfig prometheusConfig;

    @Autowired
    private TestRestTemplate restTemplate;

    @Rule
    public OutputCaptureRule outputCapture = new OutputCaptureRule();


    @Autowired
    @Qualifier("globalInterceptor")
    private ServerInterceptor globalInterceptor;


    @Test
    public void disabledServerTest() throws Throwable {
        assertNotNull(grpcServerRunner);
        assertNull(grpcInprocessServerRunner);
    }

    @Test
    public void interceptorsTest() throws ExecutionException, InterruptedException {

        GreeterGrpc.newFutureStub(channel)
                .sayHello(GreeterOuterClass.HelloRequest.newBuilder().setName("name").build())
                .get().getMessage();

        CalculatorGrpc.newFutureStub(channel)
                .calculate(CalculatorOuterClass.CalculatorRequest.newBuilder().setNumber1(1).setNumber2(1).build())
                .get().getResult();

        // global interceptor should be invoked once on each service
        Mockito.verify(globalInterceptor, Mockito.times(2)).interceptCall(Mockito.any(), Mockito.any(), Mockito.any());


        // log interceptor should be invoked only on GreeterService and not CalculatorService
        outputCapture.expect(containsString(GreeterGrpc.getSayHelloMethod().getFullMethodName()));
        outputCapture.expect(not(containsString(CalculatorGrpc.getCalculateMethod().getFullMethodName())));


        outputCapture.expect(containsString("I'm not Spring bean interceptor and still being invoked..."));
    }

    @Test
    public void actuatorTest() throws ExecutionException, InterruptedException {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/env", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }


    @Test
    public void testDefaultConfigurer() {
        Assert.assertEquals("Default configurer should be picked up",
                context.getBean(GRpcServerBuilderConfigurer.class).getClass(),
                GRpcServerBuilderConfigurer.class);
    }

    @Test
    public void testReflection() throws InterruptedException {
        List<String> discoveredServiceNames = new ArrayList<>();
        ServerReflectionRequest request = ServerReflectionRequest.newBuilder().setListServices("services").setHost("localhost").build();
        CountDownLatch latch = new CountDownLatch(1);
        ServerReflectionGrpc.newStub(channel).serverReflectionInfo(new StreamObserver<ServerReflectionResponse>() {
            @Override
            public void onNext(ServerReflectionResponse value) {
                List<ServiceResponse> serviceList = value.getListServicesResponse().getServiceList();
                for (ServiceResponse serviceResponse : serviceList) {
                    discoveredServiceNames.add(serviceResponse.getName());
                }
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        }).onNext(request);

        latch.await(3, TimeUnit.SECONDS);
        assertFalse(discoveredServiceNames.isEmpty());
    }


    @Test
    public void testHealthCheck() throws ExecutionException, InterruptedException {
        final HealthCheckRequest healthCheckRequest = HealthCheckRequest.newBuilder().setService(GreeterGrpc.getServiceDescriptor().getName()).build();
        final HealthGrpc.HealthFutureStub healthFutureStub = HealthGrpc.newFutureStub(channel);
        final HealthCheckResponse.ServingStatus servingStatus = healthFutureStub.check(healthCheckRequest).get().getStatus();
        assertNotNull(servingStatus);
        assertEquals(servingStatus, HealthCheckResponse.ServingStatus.SERVING);
    }

    @Override
    protected void afterGreeting() throws Exception {


        ResponseEntity<ObjectNode> metricsResponse = restTemplate.getForEntity("/actuator/metrics", ObjectNode.class);
        assertEquals(HttpStatus.OK, metricsResponse.getStatusCode());
        final String metricName = "grpc.server.calls";
        final Optional<String> containsGrpcServerCallsMetric = StreamSupport.stream(Spliterators.spliteratorUnknownSize(metricsResponse.getBody().withArray("names")
                .elements(), Spliterator.NONNULL), false)
                .map(JsonNode::asText)
                .filter(metricName::equals)
                .findFirst();
        assertThat("Should contain " + metricName,containsGrpcServerCallsMetric.isPresent());


        Callable<Long> getPrometheusMetrics = () -> {
            ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            return Stream.of(response.getBody().split(System.lineSeparator()))
                    .filter(s -> s.contains(metricName.replace('.','_')))
                    .count();
        };

        Awaitility
                .waitAtMost(Duration.ofMillis(prometheusConfig.step().toMillis() * 2))
                .until(getPrometheusMetrics,Matchers.greaterThan(0L));

    }
}
