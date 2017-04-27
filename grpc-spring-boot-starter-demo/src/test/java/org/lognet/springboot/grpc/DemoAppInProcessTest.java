package org.lognet.springboot.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ServerInterceptor;
import io.grpc.examples.CalculatorGrpc;
import io.grpc.examples.CalculatorOuterClass;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.hamcrest.*;
import org.junit.*;
import org.junit.runner.*;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class,TestConfig.class}, webEnvironment = NONE, properties = {"grpc.port=6969","grpc.inProcessServerName=inProcessTest"})
public class DemoAppInProcessTest {

    private ManagedChannel channel;

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Autowired
    @Qualifier("globalInterceptor")
    private  ServerInterceptor globalInterceptor;

    @Autowired
    private GRpcServerProperties serverProperties;


    @Before
    public void setup() {
        channel = InProcessChannelBuilder.forName(serverProperties.getInProcessServerName())
            .usePlaintext(true)
            .build();
    }

    @After
    public void tearDown() {
        channel.shutdown();
    }

    @Test
    public void simpleGreeting() throws ExecutionException, InterruptedException {


        String name ="John";
        final GreeterGrpc.GreeterBlockingStub greeterBlockingStub = GreeterGrpc.newBlockingStub(channel);
        final GreeterOuterClass.HelloRequest helloRequest =GreeterOuterClass.HelloRequest.newBuilder().setName(name).build();
        final String reply = greeterBlockingStub.sayHello(helloRequest).getMessage();
        assertNotNull(reply);
        assertTrue(String.format("Replay should contain name '%s'",name),reply.contains(name));

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
        outputCapture.expect(CoreMatchers.containsString(GreeterGrpc.METHOD_SAY_HELLO.getFullMethodName()));
        outputCapture.expect(CoreMatchers.not(CoreMatchers.containsString(CalculatorGrpc.METHOD_CALCULATE.getFullMethodName())));

    }

}
