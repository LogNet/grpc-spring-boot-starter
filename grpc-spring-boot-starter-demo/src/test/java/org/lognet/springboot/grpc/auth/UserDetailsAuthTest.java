package org.lognet.springboot.grpc.auth;


import com.google.protobuf.Empty;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.CalculatorGrpc;
import io.grpc.examples.CalculatorOuterClass;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import io.grpc.examples.SecuredCalculatorGrpc;
import io.grpc.examples.SecuredGreeterGrpc;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.security.AuthClientInterceptor;
import org.lognet.springboot.grpc.security.AuthHeader;
import org.lognet.springboot.grpc.security.EnableGrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;


@SpringBootTest(classes = DemoApp.class)
@RunWith(SpringRunner.class)
@Import({UserDetailsAuthTest.TestCfg.class})
public class UserDetailsAuthTest extends GrpcServerTestBase {


    @TestConfiguration
    static class TestCfg   extends GrpcSecurityConfigurerAdapter {
            static final String pwd="strongPassword1";
            @Bean
            public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
            }

            @Bean
            public UserDetails user(PasswordEncoder passwordEncoder) {
                return User.
                        withUsername("user1")
                        .password(passwordEncoder.encode(pwd))
                        .roles("reader")
                        .build();
            }


            @Override
            public void configure(GrpcSecurity builder) throws Exception {
                builder.authorizeRequests()
                        .methods(GreeterGrpc.getSayHelloMethod()).hasAnyRole("reader")
                        .methods(GreeterGrpc.getSayAuthOnlyHelloMethod()).hasAnyRole("reader")
                        .methods(CalculatorGrpc.getCalculateMethod()).hasAnyRole("anotherRole")
                        .withSecuredAnnotation()
                        .userDetailsService(new InMemoryUserDetailsManager(builder.getApplicationContext().getBean(UserDetails.class)));

            }




    }

    @Autowired
    private UserDetails user;


    @Test
    public void simpleAuthHeaderFormat() throws ExecutionException, InterruptedException {



        final GreeterGrpc.GreeterFutureStub greeterFutureStub = GreeterGrpc.newFutureStub(getChannel(false));
        final String reply = greeterFutureStub.sayAuthOnlyHello(Empty.newBuilder().build()).get().getMessage();
        assertNotNull("Reply should not be null",reply);
        assertTrue(String.format("Reply should contain name '%s'",user.getUsername()),reply.contains(user.getUsername()));


    }



    @Test
    public void shouldFailWithPermissionDenied() {

        final StatusRuntimeException statusRuntimeException = assertThrows(StatusRuntimeException.class, () -> {
            CalculatorGrpc
                    .newBlockingStub(selectedChanel) //auth channel
                    .calculate(CalculatorOuterClass.CalculatorRequest.newBuilder()
                    .setNumber1(1)
                    .setNumber2(1)
                    .setOperation(CalculatorOuterClass.CalculatorRequest.OperationType.ADD)
                    .build());
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.PERMISSION_DENIED));
    }


    @Test
    public void serviceLevelSecurityAuthenticationWithoutAuthorization() {


        final CalculatorOuterClass.CalculatorResponse response = SecuredCalculatorGrpc
                .newBlockingStub(selectedChanel)//auth channel
                .calculate(CalculatorOuterClass.CalculatorRequest.newBuilder()
                .setNumber1(1)
                .setNumber2(1)
                .setOperation(CalculatorOuterClass.CalculatorRequest.OperationType.ADD)
                .build());
        assertThat(response.getResult(),Matchers.is(2d));

    }

    @Test
    public void shouldFailWithUnauthenticated() {

        final StatusRuntimeException statusRuntimeException = assertThrows(StatusRuntimeException.class, () -> {
            SecuredCalculatorGrpc
                    .newBlockingStub(super.getChannel()) // channel without auth
                    .calculate(CalculatorOuterClass.CalculatorRequest.newBuilder()
                    .setNumber1(1)
                    .setNumber2(1)
                    .setOperation(CalculatorOuterClass.CalculatorRequest.OperationType.ADD)
                    .build());
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.UNAUTHENTICATED));

    }

    @Override
    protected Channel getChannel() {
       return getChannel(true);
    }

    protected Channel getChannel(boolean binaryFormat) {

        final AuthClientInterceptor interceptor = new AuthClientInterceptor(AuthHeader.builder()
                .basic(user.getUsername(),TestCfg.pwd.getBytes())
                .binaryFormat(binaryFormat)
        );
        return ClientInterceptors.intercept(super.getChannel(), interceptor);
    }


}
