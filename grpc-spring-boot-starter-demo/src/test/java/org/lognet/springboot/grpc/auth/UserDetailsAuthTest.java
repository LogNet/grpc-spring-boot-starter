package org.lognet.springboot.grpc.auth;


import com.google.protobuf.Empty;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.CalculatorGrpc;
import io.grpc.examples.CalculatorOuterClass;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.SecuredCalculatorGrpc;
import io.grpc.stub.StreamObserver;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.demo.DemoAppConfiguration;
import org.lognet.springboot.grpc.demo.NotSpringBeanInterceptor;
import org.lognet.springboot.grpc.security.AuthClientInterceptor;
import org.lognet.springboot.grpc.security.AuthHeader;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;


@SpringBootTest(classes = DemoApp.class,webEnvironment = SpringBootTest.WebEnvironment.NONE)
@RunWith(SpringRunner.class)
@Import({UserDetailsAuthTest.TestCfg.class})
public class UserDetailsAuthTest extends GrpcServerTestBase {


    @TestConfiguration
    static class TestCfg   extends GrpcSecurityConfigurerAdapter {

        @GRpcService(interceptors = NotSpringBeanInterceptor.class)
        @Secured({})
        public static class SecuredCalculatorService extends SecuredCalculatorGrpc.SecuredCalculatorImplBase{
            @Override
            public void calculate(CalculatorOuterClass.CalculatorRequest request, StreamObserver<CalculatorOuterClass.CalculatorResponse> responseObserver) {
                responseObserver.onNext(DemoAppConfiguration.CalculatorService.calculate(request));
                responseObserver.onCompleted();


            }
        }
            static final String pwd="strongPassword1";

            @Bean
            public UserDetails user() {
                return User.withDefaultPasswordEncoder()
                        .username("user1")
                        .password(pwd)
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
            final CalculatorOuterClass.CalculatorResponse response = CalculatorGrpc
                    .newBlockingStub(selectedChanel) //auth channel
                    .calculate(CalculatorOuterClass.CalculatorRequest.newBuilder()
                            .setNumber1(1)
                            .setNumber2(1)
                            .setOperation(CalculatorOuterClass.CalculatorRequest.OperationType.ADD)
                            .build());
            System.out.println(response);
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
