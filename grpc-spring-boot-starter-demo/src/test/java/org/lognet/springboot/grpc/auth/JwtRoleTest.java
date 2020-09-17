package org.lognet.springboot.grpc.auth;


import io.grpc.StatusRuntimeException;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;


@SpringBootTest(classes = DemoApp.class)
@ActiveProfiles("keycloack-test")
@RunWith(SpringRunner.class)
@Import({JwtRoleTest.TestCfg.class})
public class JwtRoleTest extends JwtAuthBaseTest {





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
                        .authenticationProvider(JwtAuthProviderFactory.forRoles(jwtDecoder));

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

}
