package org.lognet.springboot.grpc.auth;


import io.grpc.*;
import io.grpc.examples.*;
import io.grpc.examples.custom.Custom;
import io.grpc.examples.custom.CustomServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.lognet.springboot.grpc.auth.ExcludeMethodAuthTest.TestCfg.pwd;


@SpringBootTest(classes = DemoApp.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@RunWith(SpringRunner.class)
@Import({ExcludeMethodAuthTest.TestCfg.class})
public class ExcludeMethodAuthTest extends GrpcServerTestBase {


    @TestConfiguration
    static class TestCfg extends GrpcSecurityConfigurerAdapter {

        @GRpcService
        static class CustomService extends CustomServiceGrpc.CustomServiceImplBase {

            @Override
            public void custom(Custom.CustomRequest request, StreamObserver<Custom.CustomReply> responseObserver) {
                responseObserver.onNext(Custom.CustomReply.newBuilder().build());
                responseObserver.onCompleted();
            }
            @Override
            public void anotherCustom(Custom.CustomRequest request, StreamObserver<Custom.CustomReply> responseObserver) {
                responseObserver.onNext(Custom.CustomReply.newBuilder().build());
                responseObserver.onCompleted();
            }
        }

        static final String pwd = "strongPassword1";

        @Bean
        public static UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(user(), admin());
        }

        @Bean
        public static UserDetails user() {
            return User.withDefaultPasswordEncoder()
                    .username("user")
                    .password(pwd)
                    .authorities("ROLE_user")
                    .build();
        }

        @Bean
        public static UserDetails admin() {
            return User.withDefaultPasswordEncoder()
                    .username("admin")
                    .password(pwd)
                    .authorities("ROLE_admin")
                    .build();
        }


        @Override
        public void configure(GrpcSecurity builder) throws Exception {
            builder
                    .authorizeRequests()
                    .methods(CustomServiceGrpc.getCustomMethod()).hasAnyRole("admin")
                    .anyMethodExcluding(CustomServiceGrpc.getCustomMethod()).hasAnyRole("user");

        }


    }

    @Autowired
    private UserDetails user;
    @Autowired
    private UserDetails admin;

    @Test
    public void userAuthTest()  {


        Channel userChannel = authChannel(getChannel(),user);
        final CustomServiceGrpc.CustomServiceBlockingStub  userStub = CustomServiceGrpc.newBlockingStub(userChannel);
        Custom.CustomReply reply = userStub.anotherCustom(Custom.CustomRequest.newBuilder().build());
        assertNotNull("Reply should not be null", reply);

        final StatusRuntimeException statusRuntimeException = assertThrows(StatusRuntimeException.class, () -> {
             userStub.custom(Custom.CustomRequest.newBuilder().build());
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.PERMISSION_DENIED));

    }

    @Test
    public void adminAuthTest()  {

        Channel adminChannel = authChannel(getChannel(),admin);
        final CustomServiceGrpc.CustomServiceBlockingStub  userStub = CustomServiceGrpc.newBlockingStub(adminChannel);
        Custom.CustomReply reply = userStub.custom(Custom.CustomRequest.newBuilder().build());
        assertNotNull("Reply should not be null", reply);

        final StatusRuntimeException statusRuntimeException = assertThrows(StatusRuntimeException.class, () -> {
            userStub.anotherCustom(Custom.CustomRequest.newBuilder().build());
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.PERMISSION_DENIED));

    }

    @Override
    protected GreeterGrpc.GreeterFutureStub beforeGreeting(GreeterGrpc.GreeterFutureStub stub) {
        AuthCallCredentials callCredentials = new AuthCallCredentials(AuthHeader.builder().basic(user.getUsername(), pwd.getBytes()));
        return stub.withCallCredentials(callCredentials);
    }

    protected Channel authChannel(Channel channel, UserDetails user) {

        final AuthClientInterceptor interceptor = new AuthClientInterceptor(AuthHeader.builder()
                .basic(user.getUsername(), pwd.getBytes())
                .binaryFormat(true)
        );
        return ClientInterceptors.intercept(super.getChannel(), interceptor);
    }


}
