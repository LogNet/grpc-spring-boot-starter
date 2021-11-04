package org.lognet.springboot.grpc.auth;


import com.google.protobuf.Empty;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.SecuredGreeterGrpc;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GRpcErrorHandler;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.security.AuthCallCredentials;
import org.lognet.springboot.grpc.security.AuthHeader;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@SpringBootTest(classes = DemoApp.class)
@RunWith(SpringRunner.class)
@Import({SecurityInterceptorTest.TestCfg.class})
public class SecurityInterceptorTest extends GrpcServerTestBase {

    @TestConfiguration
    static class TestCfg   extends GrpcSecurityConfigurerAdapter {
        @Override
        public void configure(GrpcSecurity builder) throws Exception {
            builder.authorizeRequests()
                   .withSecuredAnnotation()
                    .userDetailsService(new InMemoryUserDetailsManager(
                            User.withDefaultPasswordEncoder()
                                    .username("user")
                                    .password("user")
                                    .authorities("SCOPE_profile")
                                    .build()
                    ));
        }
        @GRpcGlobalInterceptor
        @Bean
        public ServerInterceptor customInterceptor(){
            return new ServerInterceptor() {
                @Override
                public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                    if(SecuredGreeterGrpc.getSayAuthHello2Method().equals(call.getMethodDescriptor())) {
                        final Status status = Status.ALREADY_EXISTS;
                        call.close(status, new Metadata());
                        throw status.asRuntimeException();
                    }
                    return next.startCall(call,headers);
                }
            };
        }

    }

    @SpyBean
    private GRpcErrorHandler errorHandler;

    @Test
    public void originalCustomInterceptorStatusIsPreserved() {

        final StatusRuntimeException statusRuntimeException = Assert.assertThrows(StatusRuntimeException.class, () -> {
            SecuredGreeterGrpc.newBlockingStub(selectedChanel)
                    .withCallCredentials(userCredentials())
                    .sayAuthHello2(Empty.newBuilder().build()).getMessage();
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.ALREADY_EXISTS));
        verifyZeroInteractions(errorHandler);
    }
    @Test
    public void unsupportedAuthSchemeShouldThrowUnauthenticatedException() {
        AuthCallCredentials callCredentials = new AuthCallCredentials(
                AuthHeader.builder()
                        .authScheme("custom")
                        .tokenSupplier(() -> ByteBuffer.wrap("dummy".getBytes()))
        );

        final StatusRuntimeException statusRuntimeException = Assert.assertThrows(StatusRuntimeException.class, () -> {
            SecuredGreeterGrpc.newBlockingStub(selectedChanel)
                    .withCallCredentials(callCredentials)
                    .sayAuthHello2(Empty.newBuilder().build()).getMessage();
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.UNAUTHENTICATED));
        verify(errorHandler).handle(any(),eq(Status.UNAUTHENTICATED), any(),any(),any());
    }


    private  AuthCallCredentials userCredentials(){
        return new AuthCallCredentials(
                AuthHeader.builder()
                        .basic("user","user".getBytes(StandardCharsets.UTF_8))
        );
    }
}
