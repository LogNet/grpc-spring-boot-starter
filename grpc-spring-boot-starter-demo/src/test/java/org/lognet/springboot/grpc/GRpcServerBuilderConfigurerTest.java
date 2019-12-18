package org.lognet.springboot.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.CalculatorGrpc;
import io.grpc.examples.CalculatorOuterClass;
import org.hamcrest.CoreMatchers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutionException;

import static org.lognet.springboot.grpc.TestConfig.CUSTOM_EXECUTOR_MESSAGE;


/**
 * Created by 310242212 on 21-Dec-16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class,TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE
        ,properties = {"grpc.port=7777","grpc.shutdownGrace=-1"})
@ActiveProfiles(profiles = {"customServerBuilder"})
public class GRpcServerBuilderConfigurerTest {

    private ManagedChannel channel;

    @Autowired
    private GRpcServerBuilderConfigurer configurer;

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Before
    public void setup() {
        channel = ManagedChannelBuilder.forAddress("localhost", 7777)
                .usePlaintext()
                .build();
    }

    @After
    public void tearDown() {
        channel.shutdown();
    }

    @Test
    public void customServerBuilderTest() throws ExecutionException, InterruptedException {


        Assert.assertNotEquals("Custom configurer should be picked up", configurer.getClass(),GRpcServerBuilderConfigurer.class);

        double result = CalculatorGrpc.newFutureStub(channel)
                .calculate(CalculatorOuterClass.CalculatorRequest.newBuilder()
                        .setNumber1(1.0)
                        .setNumber2(1.0)
                        .setOperation(CalculatorOuterClass.CalculatorRequest.OperationType.ADD)
                        .build())
                .get().getResult();
        Assert.assertEquals(2.0,result,0.0);

        // expect invocation via custom executor
        outputCapture.expect(CoreMatchers.containsString(CUSTOM_EXECUTOR_MESSAGE));
    }
}
