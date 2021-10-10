package org.lognet.springboot.grpc.consul;


import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import com.pszymczyk.consul.ConsulProcess;
import com.pszymczyk.consul.ConsulStarterBuilder;
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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.lognet.springboot.grpc.autoconfigure.consul.ServiceRegistrationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.SocketUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;


@ActiveProfiles("consul-test")
public abstract class ConsulRegistrationBaseTest {
    private static ConsulProcess consul;

    @BeforeClass
    public static void startConsul() {
        int port = SocketUtils.findAvailableTcpPort();


        consul = ConsulStarterBuilder.consulStarter().withHttpPort(port).build().start();
        System.setProperty("spring.cloud.consul.port", String.valueOf(port));

    }

    @AfterClass
    public static void clear() {
        System.clearProperty("spring.cloud.consul.port");
        Optional.ofNullable(consul).ifPresent(ConsulProcess::close);

    }


    @Autowired
    protected DiscoveryClient discoveryClient;

    @Autowired
    protected ConfigurableApplicationContext applicationContext;

    protected final String serviceId = "grpc-grpc-demo";

    protected ConsulClient consulClient;
    private ManagedChannel channel;

    @Before
    public void setUp() throws Exception {


        consulClient = new ConsulClient("localhost", Integer.parseInt(System.getProperty("spring.cloud.consul.port")));


        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);

        final ServiceRegistrationMode registrationMode = applicationContext.getBean(GRpcServerProperties.class)
                .getConsul().getRegistrationMode();

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
        applicationContext.stop();
    }

    @Test
    public void contextLoads() {

        int minExpectedRegistrations;
        switch (applicationContext.getBean(GRpcServerProperties.class)
                .getConsul()
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
                .atMost(Duration.ofSeconds(20))
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
