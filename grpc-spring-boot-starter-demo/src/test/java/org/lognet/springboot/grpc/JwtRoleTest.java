package org.lognet.springboot.grpc;


import io.grpc.*;
import io.grpc.examples.CalculatorGrpc;
import io.grpc.examples.CalculatorOuterClass;
import io.grpc.examples.GreeterGrpc;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.security.EnableGrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.lognet.springboot.grpc.security.jwt.JwtAuthProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;


@SpringBootTest(classes = DemoApp.class)
@ActiveProfiles("keycloack-test")
@RunWith(SpringRunner.class)
@Import({JwtRoleTest.TestCfg.class})
public class JwtRoleTest extends GrpcServerTestBase {


    @Value("${embedded.keycloak.auth-server-url:}")
    private String authServerUrl;


    @TestConfiguration
    static class TestCfg {

        @EnableGrpcSecurity
        public class DemoGrpcSecurityConfig extends GrpcSecurityConfigurerAdapter {

            @Autowired
            private JwtDecoder jwtDecoder;

            @Override
            public void configure(GrpcSecurity builder) throws Exception {

                super.configure(builder);
                builder.authorizeRequests()
                        .methods(GreeterGrpc.getSayHelloMethod()).hasAnyRole("reader")
                        .methods(CalculatorGrpc.getCalculateMethod()).hasAnyRole("anotherRole")
                        .and()
                        .authenticationProvider(JwtAuthProviderFactory.withRoles(jwtDecoder));

            }


        }

    }


    @Test
    public void concurrencyTest() throws InterruptedException, ExecutionException {
        int concurrency = 501;

        final ExecutorService executorService = Executors.newFixedThreadPool(concurrency);
        final CyclicBarrier barrier = new CyclicBarrier(concurrency);
        final CountDownLatch endCountDownLatch = new CountDownLatch(concurrency);

        AtomicInteger shouldSucceed  = new AtomicInteger();
        AtomicInteger shouldFail  = new AtomicInteger();

        final   List<Future<Boolean>> result = Stream.iterate(0, i -> i + 1)
                .limit(concurrency)
                .map(i ->
                        new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                System.out.println("About to start call  "+i);
                                barrier.await();
                                System.out.println("Start call  "+i);
                                try {
                                    if (i % 2 == 0) {
                                        shouldSucceed.incrementAndGet();
                                        simpleGreeting(); //should succeed
                                    } else { //should fail
                                        shouldFail.incrementAndGet();
                                        CalculatorGrpc.newBlockingStub(selectedChanel).calculate(CalculatorOuterClass.CalculatorRequest.newBuilder()
                                                .setNumber1(1)
                                                .setNumber2(1)
                                                .setOperation(CalculatorOuterClass.CalculatorRequest.OperationType.ADD)
                                                .build());
                                    }
                                    return true;
                                } catch (Exception e) {
                                    return false;
                                }finally {
                                    System.out.println("Call  "+i+" finished");
                                    endCountDownLatch.countDown();
                                }
                            }
                        })
                .map(executorService::submit)
                .collect(Collectors.toList());


        endCountDownLatch.await();
        int failed=0, succeeded=0;
        for(Future<Boolean> res: result ){
            if(res.get()){
                ++succeeded;
            }else {
                ++failed;
            }
        }
        assertThat(succeeded,Matchers.is(shouldSucceed.get()));
        assertThat(failed,Matchers.is(shouldFail.get()));







    }

    @Test
    public void shouldFail() {

        final StatusRuntimeException statusRuntimeException = assertThrows(StatusRuntimeException.class, () -> {
            CalculatorGrpc.newBlockingStub(selectedChanel).calculate(CalculatorOuterClass.CalculatorRequest.newBuilder()
                    .setNumber1(1)
                    .setNumber2(1)
                    .setOperation(CalculatorOuterClass.CalculatorRequest.OperationType.ADD)
                    .build());
        });
        assertThat(statusRuntimeException.getMessage(), Matchers.containsString("UNAUTHENTICATED"));


    }

    @Override
    protected Channel getChannel() {
        String token = generateToken();

        return ClientInterceptors.intercept(super.getChannel(), new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel next) {
                return new ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT>(next.newCall(methodDescriptor, callOptions)) {
                    @Override
                    protected void checkedStart(Listener<RespT> responseListener, Metadata headers) throws Exception {
                        headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + token);
                        delegate().start(responseListener, headers);
                    }
                };
            }
        });
    }


    private String generateToken() {
        if (authServerUrl.isEmpty()) {
            return UUID.randomUUID().toString();
        }

        final LinkedMultiValueMap<String, String> req = new LinkedMultiValueMap<>();


        req.add("client_id", "any-client");
        req.add("client_secret", "08f64721-7fef-4d8b-a0fc-8f940a621451");
        req.add("grant_type", "password");
        req.add("username", "keycloak-test");
        req.add("password", "123Start!");


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(authServerUrl));
        final ResponseEntity<String> response = restTemplate
                .postForEntity("/realms/test-realm/protocol/openid-connect/token", new HttpEntity<>(req, headers), String.class);
        try {
            return new ObjectMapper().readTree(response.getBody())
                    .at("/access_token")
                    .asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
