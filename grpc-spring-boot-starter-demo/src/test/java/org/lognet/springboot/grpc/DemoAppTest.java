package org.lognet.springboot.grpc;

import io.grpc.ServerInterceptor;
import io.grpc.examples.CalculatorGrpc;
import io.grpc.examples.CalculatorOuterClass;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Created by alexf on 28-Jan-16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class, TestConfig.class}, webEnvironment = RANDOM_PORT
        , properties = {
        "grpc.shutdownGrace=-1",
        "spring.main.web-application-type=servlet"
})
@ActiveProfiles({"disable-security"})

public class DemoAppTest extends GrpcServerTestBase {




    @Rule
    public OutputCaptureRule outputCapture = new OutputCaptureRule();


    @Autowired
    @Qualifier("globalInterceptor")
    private ServerInterceptor globalInterceptor;


    @Test
    public void disabledServerTest()  {
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
    public void testDefaultConfigurer() {
        assertThat(context.getBean(GRpcServerBuilderConfigurer.class),isA(GRpcServerBuilderConfigurer.class));
    }







}
