package org.lognet.springboot.grpc;

import io.grpc.*;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.security.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE)
@ActiveProfiles("security-interceptor-precedence")
@Import(SecurityInterceptorPrecedenceTest.TestCfg.class)
public class SecurityInterceptorPrecedenceTest extends GrpcServerTestBase {

    private static List<Interceptor> calledInterceptors = new ArrayList<>();

    enum Interceptor {
        BUILTIN_SECURITY, USER_DEFINED
    }

    @Override
    protected void afterGreeting() {
        assertThat(calledInterceptors).containsExactly(Interceptor.USER_DEFINED, Interceptor.BUILTIN_SECURITY);
    }

    @Override
    protected Channel getChannel() {
        final AuthClientInterceptor interceptor = new AuthClientInterceptor(AuthHeader.builder()
                .basic("test", "test".getBytes())
                .binaryFormat(true)
        );
        return ClientInterceptors.intercept(super.getChannel(), interceptor);
    }

    @TestConfiguration
    @EnableGrpcSecurity
    public static class TestCfg extends GrpcSecurityConfigurerAdapter {

        @Override
        public void configure(GrpcSecurity builder) throws Exception {
            builder.authorizeRequests()
                    .anyMethod().authenticated()
                    .withInterceptorPrecedence(1)
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
                    });
        }

        @Bean
        @GRpcGlobalInterceptor
        public ServerInterceptor userDefinedInterceptor(){
            return new UserDefinedInterceptor();
        }

        @Order(0)
        static class UserDefinedInterceptor implements ServerInterceptor {

            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                calledInterceptors.add(Interceptor.USER_DEFINED);
                return next.startCall(call, headers);
            }
        }
    }

}
