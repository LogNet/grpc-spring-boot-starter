package org.lognet.springboot.grpc.health;

import io.grpc.examples.GreeterGrpc;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.reflection.v1alpha.ServiceResponse;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.Container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class, CustomManagedHealthStatusServiceTest.Cfg.class}, webEnvironment = NONE,
        properties = "grpc.enableReflection=true")
@ActiveProfiles("disable-security")
@Slf4j
public class CustomManagedHealthStatusServiceTest extends GrpcServerTestBase {

    @Rule
    public GrpcHealthProbeContainer grpcHealthProbe = new GrpcHealthProbeContainer();

    @TestConfiguration
    static class Cfg {
        @GRpcService
        static class MyCustomHealthStatusService extends DefaultHealthStatusService {
            @Getter
            private final ArrayList<String> registeredServices = new ArrayList<>();

            @Getter
            private final ArrayList<String> checkedServices = new ArrayList<>();

            @Override
            public void setStatus(String service, HealthCheckResponse.ServingStatus status) {
                synchronized (registeredServices) {
                    registeredServices.add(service);
                }
                super.setStatus(service, status);
            }

            @Override
            public void check(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
                synchronized (checkedServices) {
                    checkedServices.add(request.getService());
                }
                super.check(request, responseObserver);
            }
        }

    }

    @SpyBean
    private ManagedHealthStatusService healthStatusManager;

    @Test
    @DirtiesContext
    public void contextLoads() {
        assertThat(healthStatusManager, isA(Cfg.MyCustomHealthStatusService.class));
    }

    @Test
    @DirtiesContext
    public void grpcHealthProbeTest() throws InterruptedException, IOException {


        Cfg.MyCustomHealthStatusService healthManager = (Cfg.MyCustomHealthStatusService) healthStatusManager;

        assertThat(healthManager.getRegisteredServices(), hasSize(greaterThan(0)));
        final List<String> discovered = discoverServicesNames();

        assertThat(discovered, containsInAnyOrder(healthManager.getRegisteredServices().toArray()));

        String addressParameter = String.format("-addr=%s:%d",
                new InetUtils(new InetUtilsProperties()).findFirstNonLoopbackHostInfo().getIpAddress(),
                getPort());


        ArrayList<String> allServices = new ArrayList<>(discovered);
        allServices.add("");

        for (String serviceName : allServices) {
            final Container.ExecResult execResult = grpcHealthProbe
                    .execInContainer("/bin/grpc_health_probe",
                            addressParameter,
                            "-service=" + serviceName
                    );

            assertThat(execResult.getExitCode(), is(0));

        }
        assertThat(healthManager.getCheckedServices(), containsInAnyOrder(allServices.toArray()));


        final Container.ExecResult execResult = grpcHealthProbe
                .execInContainer("/bin/grpc_health_probe",
                        addressParameter,
                        "-service=blah"
                );

        assertThat(execResult.getExitCode(), Matchers.not(0));



    }

    @Test
    @DirtiesContext
    public void testHealthCheck() throws ExecutionException, InterruptedException {
        final HealthCheckRequest healthCheckRequest = HealthCheckRequest.newBuilder().setService(GreeterGrpc.getServiceDescriptor().getName()).build();
        final HealthGrpc.HealthFutureStub healthFutureStub = HealthGrpc.newFutureStub(channel);
        final HealthCheckResponse.ServingStatus servingStatus = healthFutureStub.check(healthCheckRequest).get().getStatus();

        assertThat(servingStatus, is(HealthCheckResponse.ServingStatus.SERVING));

        Mockito.verify(healthStatusManager, Mockito.atLeast(1))
                .setStatus(Mockito.any(String.class), Mockito.eq(HealthCheckResponse.ServingStatus.SERVING));
    }

    @Test
    public void testReflection() throws InterruptedException {

        assertThat(discoverServicesNames(), Matchers.not(Matchers.empty()));
    }

    private List<String> discoverServicesNames() throws InterruptedException {
        List<String> discoveredServiceNames = new ArrayList<>();
        ServerReflectionRequest request = ServerReflectionRequest.newBuilder().setListServices("services").setHost("localhost").build();
        CountDownLatch latch = new CountDownLatch(1);
        ServerReflectionGrpc.newStub(channel).serverReflectionInfo(new StreamObserver<ServerReflectionResponse>() {
            @Override
            public void onNext(ServerReflectionResponse value) {
                List<ServiceResponse> serviceList = value.getListServicesResponse().getServiceList();
                for (ServiceResponse serviceResponse : serviceList) {

                    final String serviceName = serviceResponse.getName();
                    if (
                            !serviceName.equals(ServerReflectionGrpc.getServiceDescriptor().getName()) &&
                                    !serviceName.equals(HealthGrpc.getServiceDescriptor().getName())
                    ) {

                        discoveredServiceNames.add(serviceName);
                    }
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
        return discoveredServiceNames;
    }
}
