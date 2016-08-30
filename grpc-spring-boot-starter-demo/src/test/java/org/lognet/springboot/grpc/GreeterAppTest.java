package org.lognet.springboot.grpc;

import static org.junit.Assert.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.GreeterApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;

/**
 * Created by alexf on 28-Jan-16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = GreeterApp.class, webEnvironment = DEFINED_PORT)
public class GreeterAppTest {

    private ManagedChannel channel;

    @Before
    public void setup() {
        channel = ManagedChannelBuilder.forAddress("localhost", 6565)
            .usePlaintext(true)
            .build();
    }

    @After
    public void tearDown() {
        channel.shutdown();
    }

    @Test
    public void simpleGreeting() throws ExecutionException, InterruptedException {
        String name = "John";
        final GreeterGrpc.GreeterFutureStub greeterFutureStub = GreeterGrpc.newFutureStub(channel);

        final GreeterOuterClass.HelloRequest helloRequest =GreeterOuterClass.HelloRequest.newBuilder().setName(name).build();
        final String reply = greeterFutureStub.sayHello(helloRequest).get().getMessage();
        assertNotNull(reply);
        assertTrue(String.format("Reply should contain name '%s'",name),reply.contains(name));
    }

    @Test
    public void actuatorTest() throws ExecutionException, InterruptedException {
        final TestRestTemplate template = new TestRestTemplate();

        ResponseEntity<String> response = template.getForEntity("http://localhost:8080/env", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

}
