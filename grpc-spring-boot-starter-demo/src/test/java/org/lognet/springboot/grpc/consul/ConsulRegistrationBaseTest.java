package org.lognet.springboot.grpc.consul;


import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import io.grpc.health.v1.HealthGrpc;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lognet.springboot.grpc.autoconfigure.consul.GrpcConsulProperties;
import org.lognet.springboot.grpc.autoconfigure.consul.ServiceRegistrationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;


@ActiveProfiles("consul-test")
@DirtiesContext
public abstract class ConsulRegistrationBaseTest {

    @Autowired
    protected ConsulDiscoveryClient discoveryClient;

    @Autowired
    protected ConfigurableApplicationContext applicationContext;

    protected final String serviceId = "grpc-grpc-demo";

    @Autowired
    protected ConsulClient consulClient;

    private ManagedChannel channel;

    @Before
    public void setUp() throws Exception {


        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);

        final ServiceRegistrationMode registrationMode = applicationContext.getBean(GrpcConsulProperties.class)
                .getRegistrationMode();

        if(!ServiceRegistrationMode.NOOP.equals(registrationMode)) {


            assertThat(instances, Matchers.not(Matchers.empty()));

            ServiceInstance serviceInstance = instances.get(0);

            channel = ManagedChannelBuilder.forAddress(serviceInstance.getHost(), serviceInstance.getPort())
                    .usePlaintext()
                    .build();

            final GreeterGrpc.GreeterFutureStub greeterFutureStub = GreeterGrpc.newFutureStub(channel);
            final GreeterOuterClass.HelloRequest helloRequest = GreeterOuterClass.HelloRequest.newBuilder().setName("Bob").build();
            final String reply = greeterFutureStub.sayHello(helloRequest).get().getMessage();
            assertThat("Reply should not be null", reply, Matchers.notNullValue(String.class));
        }
    }

    @After
    public void tearDown() throws Exception {
        if(null!=channel) {
            channel.shutdownNow();
            channel.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    public void contextLoads() {

        int minExpectedRegistrations;
        switch (applicationContext.getBean(GrpcConsulProperties.class)
                .getRegistrationMode()) {
            case STANDALONE_SERVICES:
                minExpectedRegistrations = getServicesDefinitions().size();
                break;
            case NOOP:
                minExpectedRegistrations = 0;
                break;
            default:
                minExpectedRegistrations = 1;
        }


        final List<HealthService> healthServices = Awaitility.await()
                .atMost(Duration.ofMinutes(1))
                .pollInterval(Duration.ofSeconds(3))
                .until(() -> consulClient.getHealthServices(serviceId, HealthServicesRequest.newBuilder()
                                        .setPassing(true)
                                        .setQueryParams(QueryParams.DEFAULT)
                                        .build())
                                .getValue()

                        , Matchers.hasSize(Matchers.greaterThanOrEqualTo(minExpectedRegistrations)));

        doTest(healthServices);


    }

    abstract void doTest(List<HealthService> healthServices);

    protected List<ServerServiceDefinition> getServicesDefinitions() {
        return applicationContext.getBeansOfType(BindableService.class)
                .values()
                .stream()
                .map(BindableService::bindService)
                .filter(s -> !s.getServiceDescriptor().equals(HealthGrpc.getServiceDescriptor()))
                .collect(Collectors.toList());
    }
}
