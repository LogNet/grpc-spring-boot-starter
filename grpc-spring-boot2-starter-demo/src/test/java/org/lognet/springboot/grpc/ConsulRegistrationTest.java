package org.lognet.springboot.grpc;

import com.pszymczyk.consul.junit.ConsulResource;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = DemoApp.class, properties = {"spring.cloud.config.enabled:false"})
public class ConsulRegistrationTest {

    @ClassRule
    public static final ConsulResource consul = new ConsulResource();

    @Autowired
    private DiscoveryClient discoveryClient;

    @Test
    public void contextLoads() throws ExecutionException, InterruptedException {
        List<ServiceInstance> instances = discoveryClient.getInstances("grpc-grpc-demo");
        assertFalse(instances.isEmpty());

        ServiceInstance serviceInstance = instances.get(0);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(serviceInstance.getHost(), serviceInstance.getPort())
                .usePlaintext()
                .build();

        final GreeterGrpc.GreeterFutureStub greeterFutureStub = GreeterGrpc.newFutureStub(channel);
        final GreeterOuterClass.HelloRequest helloRequest =GreeterOuterClass.HelloRequest.newBuilder().setName("Bob").build();
        final String reply = greeterFutureStub.sayHello(helloRequest).get().getMessage();
        assertNotNull("Replay should not be null",reply);


    }
}
