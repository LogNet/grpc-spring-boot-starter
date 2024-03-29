package org.lognet.springboot.grpc.reactive;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.reactor.ReactiveGreeterGrpc;
import io.grpc.examples.reactor.ReactiveHelloRequest;
import io.grpc.examples.reactor.ReactiveHelloResponse;
import io.grpc.examples.reactor.ReactorReactiveGreeterGrpc;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsIn;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.auth.JwtAuthBaseTest;
import org.lognet.springboot.grpc.auth.JwtRoleTest;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;


@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DemoApp.class)
@ActiveProfiles({"keycloack-test", "r2dbc-test"})
@DirtiesContext
public class ReactiveDemoTest extends JwtAuthBaseTest {

    @Test
    public void grpcGreetTest() {
        String shrek = "Shrek";
        String message = ReactiveGreeterGrpc.newBlockingStub(getChannel())
                .greet(ReactiveHelloRequest.newBuilder().setName(shrek).build())
                .getMessage();
        assertThat(message, containsString(shrek));

    }

    @Test
    public void reactorGreetTest() {
        String shrek = "Shrek";
        ReactiveHelloResponse helloResponse = ReactorReactiveGreeterGrpc.newReactorStub(getChannel())
                .greet(simpleRequest(shrek))
                .block(Duration.ofSeconds(10));
        assertThat(helloResponse, notNullValue());

        assertThat(helloResponse.getMessage(), containsString(shrek));

    }

    @Test
    public void reactorGreetFailureTest() {
        String shrek = "Wolf";
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> {

            ReactorReactiveGreeterGrpc.newReactorStub(getChannel())
                    .greet(simpleRequest(shrek))
                    .block(Duration.ofSeconds(10));
        });
        assertThat(e.getMessage(), containsStringIgnoringCase("not welcome"));
        assertThat(e.getStatus().getCode(), is(Status.INVALID_ARGUMENT.getCode()));


    }

    @Test
    public void reactorMultiGreerTest() {
        String shrek = "Shrek";
        List<ReactiveHelloResponse> greets = ReactorReactiveGreeterGrpc.newReactorStub(getChannel())
                .multiGreet(simpleRequest(shrek))
                .collectList()
                .block(Duration.ofSeconds(10));

        assertThat(greets, notNullValue());
        assertThat(greets, hasSize(shrek.length()));

        assertThat(greets.stream().map(ReactiveHelloResponse::getMessage).toList(), everyItem(containsString(shrek)));

    }

    @Test
    public void reactorBidiGreerTest() {
        String[] names = new String[]{
                "Shrek",
                "Fiona",
                "Robin",
                "Christopher"
        };
        List<ReactiveHelloResponse> greets = ReactorReactiveGreeterGrpc.newReactorStub(getChannel())
                .streamGreet(
                        Flux.fromStream(Arrays.stream(names).map(this::simpleRequest))
                )
                .collectList()
                .block(Duration.ofSeconds(10));

        assertThat(greets, notNullValue());
        assertThat(greets, hasSize(name.length()));

        assertThat(greets.stream().map(ReactiveHelloResponse::getMessage).toList(),
                Matchers.everyItem(new IsIn<>(names) {
                    @Override
                    public boolean matches(Object actual) {
                        return Arrays.stream(names)
                                .anyMatch(a -> actual.toString().contains(a));
                    }
                })
        );

    }

    private ReactiveHelloRequest simpleRequest(String name) {
        return ReactiveHelloRequest.newBuilder().setName(name).build();
    }
}