package org.lognet.springboot.grpc;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleConfig;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE, properties = {"grpc.port=0"})
@ActiveProfiles("measure")
public class GrpcMeterTest extends GrpcServerTestBase {
    @Autowired
    private MeterRegistry registry;

    @Autowired
    private SimpleConfig registryConfig;

    @Before
    public void setUp()  {
        registry.clear();
    }

    @Override
    protected void afterGreeting()  {


        final Timer timer = registry.find("grpc.server.calls").timer();
        assertThat(timer,notNullValue(Timer.class));

        Awaitility
                .waitAtMost(Duration.ofMillis(registryConfig.step().toMillis() * 2))
                .until(timer::count,greaterThan(0L));

        assertThat(timer.max(TimeUnit.MILLISECONDS),greaterThan(0d));
        assertThat(timer.mean(TimeUnit.MILLISECONDS),greaterThan(0d));
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS),greaterThan(0d));
    }
}
