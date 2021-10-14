package org.lognet.springboot.grpc.auth;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.SecuredGreeterGrpc;
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
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE)
@Import(CustomSecurityTest.TestConfig.class)
public class CustomSecurityTest extends GrpcServerTestBase {
    private final static String MY_CUSTOM_SCHEME_NAME = "custom";

    @TestConfiguration
    static class TestConfig {

        @EnableGrpcSecurity
        public class DemoGrpcSecurityConfig extends GrpcSecurityConfigurerAdapter {


            @Override
            public void configure(GrpcSecurity builder) throws Exception {
                builder.authorizeRequests()
                        .withSecuredAnnotation()
                        .authenticationSchemeSelector(scheme ->
                                Optional.of(scheme.toString())
                                        .filter(s -> s.startsWith(MY_CUSTOM_SCHEME_NAME))
                                        .map(s -> s.substring(MY_CUSTOM_SCHEME_NAME.length() + 1))
                                        .map(token -> {
                                            final String[] chunks = token.split("#");
                                            return new TestingAuthenticationToken(token.split("#")[0], null, "SCOPE_" + chunks[1]);
                                        })
                        )
                        .authenticationProvider(new TestingAuthenticationProvider());
            }

        }

    }


    @Test
    public void customSchemeTest() throws ExecutionException, InterruptedException {
        String userName = "userToken1";
        String reply = invoke(userName, "profile");
        assertNotNull("Reply should not be null", reply);
        assertTrue(String.format("Reply should contain name '%s'", userName), reply.contains(userName));
    }

    @Test
    public void customSchemeAccessDeniedTest() {

        final StatusRuntimeException e = assertThrows(StatusRuntimeException.class,
                () -> invoke("userToken1", "deniedProfile")
        );
        assertThat(e.getStatus().getCode(), Matchers.is(Status.Code.PERMISSION_DENIED));

    }

    private String invoke(String userName, String authority) {
        AuthCallCredentials callCredentials = new AuthCallCredentials(
                AuthHeader.builder().authScheme(MY_CUSTOM_SCHEME_NAME).tokenSupplier(() ->
                        ByteBuffer.wrap(String.format("%s#%s", userName, authority).getBytes()))
        );

        return SecuredGreeterGrpc.newBlockingStub(getChannel())
                .withCallCredentials(callCredentials)
                .sayAuthHello2(Empty.newBuilder().build()).getMessage();
    }
}
