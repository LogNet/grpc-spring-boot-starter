package org.lognet.springboot.grpc.consul;

import com.ecwid.consul.v1.health.model.Check;
import com.ecwid.consul.v1.health.model.HealthService;
import org.hamcrest.Matchers;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;


@SpringBootTest(classes = DemoApp.class)
@RunWith(SpringRunner.class)
public class ConsulDefaultRegistrationTest extends ConsulRegistrationBaseTest{




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
