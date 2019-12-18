package org.lognet.springboot.grpc;

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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Created by alexf on 28-Jan-16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class, TestConfig.class}, webEnvironment = RANDOM_PORT
        , properties = {"grpc.enableReflection=true",
        "grpc.port=0",
        "grpc.shutdownGrace=-1"
})
public class DemoAppTest extends GrpcServerTestBase{

    @Autowired
    private TestRestTemplate restTemplate;

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Autowired
    @Qualifier("globalInterceptor")
    private  ServerInterceptor globalInterceptor;


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
        Mockito.verify(globalInterceptor,Mockito.times(2)).interceptCall(Mockito.any(),Mockito.any(),Mockito.any());


        // log interceptor should be invoked only on GreeterService and not CalculatorService
        outputCapture.expect(containsString(GreeterGrpc.getSayHelloMethod().getFullMethodName()));
        outputCapture.expect(not(containsString(CalculatorGrpc.getCalculateMethod().getFullMethodName())));


        outputCapture.expect(containsString("I'm not Spring bean interceptor and still being invoked..."));
    }

        @Test
    public void actuatorTest() throws ExecutionException, InterruptedException {
        ResponseEntity<String> response = restTemplate.getForEntity("/env", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }


    @Test
    public void testDefaultConfigurer(){
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
}
