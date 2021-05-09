package org.lognet.springboot.grpc.auth;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.GreeterGrpc.GreeterFutureStub;
import io.grpc.examples.SecuredGreeterGrpc;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.security.AuthCallCredentials;
import org.lognet.springboot.grpc.security.AuthHeader;
import org.lognet.springboot.grpc.security.EnableGrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = DemoApp.class, properties = "spring.cloud.service-registry.auto-registration.enabled=false")
@RunWith(SpringRunner.class)
@Import({ConcurrentAuthConfigTest.TestCfg.class})
@Slf4j
public class ConcurrentAuthConfigTest extends GrpcServerTestBase {

    private static User user1 = new User("test1", "test1", Collections.EMPTY_LIST);
    private static User user2 = new User("test2", "test2", Collections.EMPTY_LIST);

    private AuthCallCredentials user1CallCredentials = new AuthCallCredentials(
            AuthHeader.builder().basic(user1.getUsername(), user1.getPassword().getBytes()));

    private AuthCallCredentials user2CallCredentials = new AuthCallCredentials(
            AuthHeader.builder().basic(user2.getUsername(), user2.getPassword().getBytes()));

    @TestConfiguration
    static class TestCfg  extends GrpcSecurityConfigurerAdapter {

            @Override
            public void configure(GrpcSecurity builder) throws Exception {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
                UserDetailsService users = new InMemoryUserDetailsManager(user1, user2);
                provider.setUserDetailsService(users);
                provider.setPasswordEncoder(NoOpPasswordEncoder.getInstance());

                builder
                        .authenticationProvider(provider)
                        .authorizeRequests()
                        .anyMethod().authenticated();
            }


    }

    @Test
    public void concurrentTest() throws InterruptedException {
        System.out.println();

        final SecuredGreeterGrpc.SecuredGreeterBlockingStub unsecuredFutureStub = SecuredGreeterGrpc
                .newBlockingStub(selectedChanel);

        final SecuredGreeterGrpc.SecuredGreeterBlockingStub securedFutureStub1 = unsecuredFutureStub
                .withCallCredentials(user1CallCredentials);

        final SecuredGreeterGrpc.SecuredGreeterBlockingStub securedFutureStub2 = unsecuredFutureStub
                .withCallCredentials(user2CallCredentials);


        int parallelTests = 10;

        List<Thread> threads = new ArrayList<>();
        // Number of threads that passed the test
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger failureCounter = new AtomicInteger(0);

        Function<Integer, Void> authenticated = i -> {
            SecuredGreeterGrpc.SecuredGreeterBlockingStub stub = null;
            User user = null;
            if (0 == i % 2) {
                stub = securedFutureStub1;
                user = user1;
            }else{
                stub = securedFutureStub2;
                user = user2;
            }
            final String reply = stub.sayAuthHello(Empty.getDefaultInstance()).getMessage();
            assertThat(reply, Matchers.containsString(user.getUsername()));
            return null;
        };
        Runnable unauthenticated = () -> {
            StatusRuntimeException err = assertThrows(StatusRuntimeException.class,
                    () -> unsecuredFutureStub.sayAuthHello(Empty.getDefaultInstance()).getMessage());
            assertEquals(Status.Code.UNAUTHENTICATED, err.getStatus().getCode());
        };

        // Check that the assertions work as is (single threaded)
        authenticated.apply(0);
        unauthenticated.run();

        for (int i = 0; i < parallelTests; i++) {
            Thread success = new Thread(() -> {

                for (int j = 0; j < 1000; j++) {
                    authenticated.apply(j);
                }
                successCounter.incrementAndGet();
                log.info("All passed");
            });
            success.setUncaughtExceptionHandler((thread, ex) -> {
                log.error("SECURITY ???", ex);
            });
            threads.add(success);

            Thread failure = new Thread(() -> {

                for (int j = 0; j < 1000; j++) {
                    unauthenticated.run();
                }
                failureCounter.incrementAndGet();
                log.info("All passed");
            });
            failure.setUncaughtExceptionHandler((thread, ex) -> {
                log.error("SECURITY BYPASSED", ex);
            });

            threads.add(failure);
        }

        Collections.shuffle(threads);
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        assertAll(() -> assertEquals(parallelTests, successCounter.get()),
                () -> assertEquals(parallelTests, failureCounter.get()));
    }

    @Override
    protected GreeterFutureStub beforeGreeting(GreeterFutureStub stub) {
        return stub.withCallCredentials(user1CallCredentials);
    }

}