package org.lognet.springboot.grpc.auth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.Optional;

import com.google.protobuf.Empty;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GRpcErrorHandler;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.security.AuthCallCredentials;
import org.lognet.springboot.grpc.security.AuthHeader;
import org.lognet.springboot.grpc.security.AuthenticationSchemeSelector;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.lognet.springboot.grpc.security.GrpcSecurityMetadataSource;
import org.lognet.springboot.grpc.security.SecurityInterceptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.junit4.SpringRunner;

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
                    .userDetailsService(new InMemoryUserDetailsManager());
        }
    }

    @SpyBean
    private GRpcErrorHandler errorHandler;

    @Test
    public void unsupportedAuthSchemeShouldThrowUnauthenticatedException() {
        AuthCallCredentials callCredentials = new AuthCallCredentials(
                AuthHeader.builder()
                        .authScheme("custom")
                        .tokenSupplier(() -> ByteBuffer.wrap("dummy".getBytes()))
        );



        final StatusRuntimeException statusRuntimeException = Assert.assertThrows(StatusRuntimeException.class, () -> {
            io.grpc.examples.SecuredGreeterGrpc.newBlockingStub(selectedChanel) 
                    .withCallCredentials(callCredentials)
                    .sayAuthHello2(Empty.newBuilder().build()).getMessage();
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.UNAUTHENTICATED));
        verify(errorHandler).handle(any(),eq(Status.UNAUTHENTICATED), any(),any(),any());
    }
}
