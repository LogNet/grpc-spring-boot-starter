package org.lognet.springboot.grpc.actuator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.micrometer.prometheus.PrometheusConfig;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.TestConfig;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Created by alexf on 28-Jan-16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class, TestConfig.class}, webEnvironment = RANDOM_PORT
        , properties = {

        "management.endpoint.health.show-details=always"
        , "management.endpoint.health.show-components=always"
        , "spring.main.web-application-type=servlet"
})
@ActiveProfiles({"disable-security", "measure"})
public class ActuatorTest extends GrpcServerTestBase {

    @Autowired
    private PrometheusConfig prometheusConfig;

    @Autowired
    private TestRestTemplate restTemplate;


    @Test
    public void actuatorEnvTest() throws ExecutionException, InterruptedException {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/env", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void actuatorGrpcTest() throws ExecutionException, InterruptedException {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/grpc", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final DocumentContext json = JsonPath.parse(response.getBody(), Configuration.builder()
                .mappingProvider(new JacksonMappingProvider())
                .jsonProvider(new JacksonJsonProvider())
                .build());
        final String[] statuses = json.read("services.*name", new TypeRef<String[]>() {});
        assertThat(statuses,Matchers.arrayWithSize(Matchers.greaterThan(0)));
        for(String s:statuses) {
            assertThat(s, Matchers.not(Matchers.blankOrNullString()));
        }

        final Integer port = json.read("port", Integer.class);
        assertThat(port,Matchers.greaterThan(0));
    }

    @Test
    public void actuatorHealthTest() throws ExecutionException, InterruptedException {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/grpc", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        final DocumentContext json = JsonPath.parse(response.getBody(), Configuration.builder()
                        .mappingProvider(new JacksonMappingProvider())
                        .jsonProvider(new JacksonJsonProvider())
                .build());
        final TypeRef<Set<String>> setOfString = new TypeRef<Set<String>>() {
        };
        final Set<String> services = json.read("components.keys()", setOfString);
        assertThat(services,Matchers.containsInAnyOrder( super.appServicesNames().toArray(new String[]{})));

        final Set<String> statuses = json.read("components.*status", setOfString);
        assertThat(statuses,Matchers.contains(Status.UP.getCode()));



    }

    @Override
    protected void afterGreeting() throws Exception {


        ResponseEntity<ObjectNode> metricsResponse = restTemplate.getForEntity("/actuator/metrics", ObjectNode.class);
        assertEquals(HttpStatus.OK, metricsResponse.getStatusCode());
        final String metricName = "grpc.server.calls";
        final Optional<String> containsGrpcServerCallsMetric = StreamSupport.stream(Spliterators.spliteratorUnknownSize(metricsResponse.getBody().withArray("names")
                        .elements(), Spliterator.NONNULL), false)
                .map(JsonNode::asText)
                .filter(metricName::equals)
                .findFirst();
        assertThat("Should contain " + metricName, containsGrpcServerCallsMetric.isPresent());


        Callable<Long> getPrometheusMetrics = () -> {
            ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            return Stream.of(response.getBody().split(System.lineSeparator()))
                    .filter(s -> s.contains(metricName.replace('.', '_')))
                    .count();
        };

        Awaitility
                .waitAtMost(Duration.ofMillis(prometheusConfig.step().toMillis() * 2))
                .until(getPrometheusMetrics, Matchers.greaterThan(0L));

    }


}
