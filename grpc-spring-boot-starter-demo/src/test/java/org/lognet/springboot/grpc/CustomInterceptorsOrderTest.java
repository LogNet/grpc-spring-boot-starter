package org.lognet.springboot.grpc;


import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.security.AuthClientInterceptor;
import org.lognet.springboot.grpc.security.AuthHeader;
import org.lognet.springboot.grpc.security.EnableGrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.lognet.springboot.grpc.security.SecurityInterceptor;
import org.lognet.springboot.grpc.validation.ValidatingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SpringBootTest(classes = DemoApp.class,
        properties = {
                "grpc.security.auth.fail-fast=false", // give validator a chance to run before failing auth
                "grpc.security.auth.interceptor-order=3", //third
                "grpc.metrics.interceptor-order=2", //second
                "grpc.validation.interceptor-order=1" //first
        })
@RunWith(SpringRunner.class)
@Import({CustomInterceptorsOrderTest.TestCfg.class})
public class CustomInterceptorsOrderTest extends GrpcServerTestBase {

    private final static List<CustomInterceptorsOrderTest.Interceptor> calledInterceptors = new ArrayList<>();

    enum Interceptor {
        BUILTIN_SECURITY, USER_DEFINED
    }


    @Autowired
    @Lazy
    private List<ServerInterceptor> interceptors;


    @Before
    public void setUp() throws Exception {
        calledInterceptors.clear();
        final List<Class<? extends ServerInterceptor>> orderedInterceptorsClasses = Arrays.asList( //according the order define by properties
                TestCfg.UserDefinedInterceptor.class,
                ValidatingInterceptor.class,
                SecurityInterceptor.class
        );
        final List<Class<?>> orderedInterceptors = interceptors
                .stream()
                .map(Object::getClass)
                .filter(orderedInterceptorsClasses::contains)
                .collect(Collectors.toList());

        assertThat(orderedInterceptors, Matchers.is(orderedInterceptorsClasses));

    }

    @Test
    public void validationShouldInvokedBeforeAuthTest() {
        final GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(super.getChannel());
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> {
            stub.helloPersonValidResponse(GreeterOuterClass.Person.newBuilder()
                    .setAge(49)// valid
                    .clearName()//invalid
                    .build());
        });

        assertThat(e.getStatus().getCode(), Matchers.is(Status.Code.INVALID_ARGUMENT));


    }

    @Override
    protected void afterGreeting() {
        assertThat(calledInterceptors, Matchers.contains(Interceptor.USER_DEFINED, Interceptor.BUILTIN_SECURITY));
    }

    @TestConfiguration
    static class TestCfg  extends GrpcSecurityConfigurerAdapter {


            static final String pwd = "strongPassword1";

            @Bean
            public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
            }

            @Bean
            public UserDetails user() {
                return User.
                        withUsername("user1")
                        .password(passwordEncoder().encode(pwd))
                        .roles("reader")
                        .build();
            }


            @Override
            public void configure(GrpcSecurity builder) throws Exception {


                builder.authorizeRequests()
                        .anyMethod().authenticated()
                        .and()
                        .authenticationProvider(new AuthenticationProvider() {
                            @Override
                            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                                calledInterceptors.add(Interceptor.BUILTIN_SECURITY);
                                Authentication fakeAuthentication = mock(Authentication.class);
                                when(fakeAuthentication.isAuthenticated()).thenReturn(true);
                                return fakeAuthentication;
                            }

                            @Override
                            public boolean supports(Class<?> authentication) {
                                return true;
                            }
                        })
                        .userDetailsService(new InMemoryUserDetailsManager(user()));
            }


        @Bean
        @GRpcGlobalInterceptor
        public ServerInterceptor userDefinedInterceptor() {
            return new UserDefinedInterceptor();
        }

        @Order(0)
        static class UserDefinedInterceptor implements ServerInterceptor {

            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                calledInterceptors.add(CustomInterceptorsOrderTest.Interceptor.USER_DEFINED);
                return next.startCall(call, headers);
            }
        }

    }

    @Autowired
    private UserDetails user;


    @Override
    protected Channel getChannel() {


        final AuthClientInterceptor interceptor = new AuthClientInterceptor(AuthHeader.builder()
                .basic(user.getUsername(), TestCfg.pwd.getBytes())
        );
        return ClientInterceptors.intercept(super.getChannel(), interceptor);
    }


}
