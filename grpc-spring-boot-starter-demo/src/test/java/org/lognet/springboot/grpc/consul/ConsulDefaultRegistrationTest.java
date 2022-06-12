package org.lognet.springboot.grpc.consul;

import com.ecwid.consul.v1.health.model.Check;
import com.ecwid.consul.v1.health.model.HealthService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.autoconfigure.consul.GrpcConsulProperties;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.discovery.ConsulServiceInstance;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;


@SpringBootTest(classes = DemoApp.class)
@RunWith(SpringRunner.class)
@ActiveProfiles("consul-grpc-config-test")
public class ConsulDefaultRegistrationTest extends ConsulRegistrationBaseTest{


    @Test
    public void consulPropertiesTest() {
        final ConsulDiscoveryProperties cloudConsulProps = applicationContext.getBean(ConsulDiscoveryProperties.class);
        assertThat(cloudConsulProps.getTags(),Matchers.contains("a","b"));
        assertThat(cloudConsulProps.getInstanceZone(),Matchers.is("zone1"));
        assertThat(cloudConsulProps.getInstanceGroup(),Matchers.nullValue(String.class));


        final ConsulDiscoveryProperties grpcConsulProperties = applicationContext.getBean(GrpcConsulProperties.class).getDiscovery();
        assertThat(grpcConsulProperties.getTags(), Matchers.hasSize(3));
        assertThat(grpcConsulProperties.getTags(), Matchers.hasItem("1"));

        assertThat(grpcConsulProperties.getInstanceZone(),Matchers.is("zone1"));
        assertThat(grpcConsulProperties.getInstanceGroup(),Matchers.is("group1"));

        final List<ServiceInstance> instances = discoveryClient.getInstances("grpc-grpc-demo");

        assertThat(instances,Matchers.hasSize(1));
        assertThat(instances.get(0),Matchers.isA(ConsulServiceInstance.class));
        ConsulServiceInstance consulServiceInstance = (ConsulServiceInstance) instances.get(0);
        final Map<String, String> metadata = consulServiceInstance.getMetadata();
        assertThat(metadata.get("secure"),Matchers.is(Boolean.FALSE.toString()));
        assertThat(metadata.get("key1"),Matchers.is("value1"));
        assertThat(consulServiceInstance.getTags(),Matchers.containsInAnyOrder("1","grpc=true","customTagName=A"));




    }

    @Override
    void doTest( List<HealthService> healthServices) {


        assertThat(healthServices,Matchers.hasSize(1));

        final long healthyGrpcServicesChecksCount = healthServices
                .stream()
                .flatMap(h -> h.getChecks().stream())
                .filter(c -> Check.CheckStatus.PASSING.equals(c.getStatus()) && c.getCheckId().contains(serviceId))
                .count();
        assertThat(healthyGrpcServicesChecksCount,Matchers.is(1L));
    }
}
