package org.lognet.springboot.grpc.consul;

import com.ecwid.consul.v1.health.model.HealthService;
import org.hamcrest.Matchers;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;


@SpringBootTest(classes = DemoApp.class,properties = "grpc.consul.registration-mode=NOOP")
@RunWith(SpringRunner.class)
@RecordApplicationEvents
public class ConsulNoopHealthCheckTest extends ConsulRegistrationBaseTest{

    @Override
    void doTest( List<HealthService> healthServices) {
        assertThat(healthServices,Matchers.hasSize(0));
    }
}
