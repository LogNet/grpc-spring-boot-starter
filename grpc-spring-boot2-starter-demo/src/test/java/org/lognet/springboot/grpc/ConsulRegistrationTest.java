package org.lognet.springboot.grpc;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.health.model.Check;
import com.ecwid.consul.v1.health.model.HealthService;
import com.pszymczyk.consul.junit.ConsulResource;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = DemoApp.class, properties = {"spring.cloud.config.enabled:false",
        "spring.cloud.consul.discovery.enabled=true",
        "spring.cloud.service-registry.auto-registration.enabled=true",
        "grpc.shutdownGrace=1"})

public class ConsulRegistrationTest {
    @ClassRule
    public static final ConsulResource consul(){
        int port = SocketUtils.findAvailableTcpPort();
        ConsulResource consulResource = new ConsulResource(port);
        System.setProperty("spring.cloud.consul.port",String.valueOf(port));
        return consulResource;
    }
    @AfterClass
    public  static void clear(){
        System.clearProperty("spring.cloud.consul.port");
    }




    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    ConfigurableApplicationContext applicationContext;

    @Test
    public void contextLoads() throws ExecutionException, InterruptedException {
        final String serviceId = "grpc-grpc-demo";
        final ConsulClient consulClient = new ConsulClient("localhost", Integer.parseInt(System.getProperty("spring.cloud.consul.port")));


        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        assertFalse(instances.isEmpty());

        ServiceInstance serviceInstance = instances.get(0);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(serviceInstance.getHost(), serviceInstance.getPort())
                .usePlaintext()
                .build();

        final GreeterGrpc.GreeterFutureStub greeterFutureStub = GreeterGrpc.newFutureStub(channel);
        final GreeterOuterClass.HelloRequest helloRequest =GreeterOuterClass.HelloRequest.newBuilder().setName("Bob").build();
        final String reply = greeterFutureStub.sayHello(helloRequest).get().getMessage();
        assertNotNull("Replay should not be null",reply);

        boolean isHealthy = false;
        for(int i=0;i<5; ++i){
            final List<HealthService> healthServices = consulClient.getHealthServices(serviceId, true, QueryParams.DEFAULT).getValue();
            isHealthy =healthServices
                    .stream()
                    .flatMap(h->h.getChecks().stream())
                    .anyMatch(c-> Check.CheckStatus.PASSING.equals(c.getStatus())&& c.getCheckId().contains(serviceId));
            if(isHealthy){
                break;
            }else{
                Thread.sleep(Duration.ofSeconds(10).toMillis());
            }
        }
        assertTrue(isHealthy);
        applicationContext.stop();
    }
}
