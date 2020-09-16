package org.lognet.springboot.grpc.auth;


import com.google.protobuf.Empty;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.SecuredGreeterGrpc;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.security.AuthCallCredentials;
import org.lognet.springboot.grpc.security.EnableGrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;


@SpringBootTest(classes = DemoApp.class)
@ActiveProfiles("keycloack-test")
@RunWith(SpringRunner.class)
@Import({PerCallDefaultAuthConfigTest.TestCfg.class})
public class PerCallDefaultAuthConfigTest extends JwtAuthBaseTest {

    private AuthCallCredentials callCredentials;
    public PerCallDefaultAuthConfigTest() {
        super(false);
        callCredentials = AuthCallCredentials.builder().bearer().tokenSupplier(this::generateToken).build();
    }

    @TestConfiguration
    static class TestCfg {

        @EnableGrpcSecurity
        public class DemoGrpcSecurityConfig extends GrpcSecurityConfigurerAdapter {

        }
    }


    @Test
    public void securedServiceTest() {

        final SecuredGreeterGrpc.SecuredGreeterBlockingStub securedFutureStub = SecuredGreeterGrpc.newBlockingStub(selectedChanel);

        final String reply = securedFutureStub
                .withCallCredentials(callCredentials)
                .sayAuthHello(Empty.getDefaultInstance()).getMessage();
        assertNotNull("Reply should not be null", reply);
        assertTrue(String.format("Reply should contain name '%s'", USER_NAME), reply.contains(USER_NAME));

    }

    @Test
    public void unauthorizedServiceTest() {

        final SecuredGreeterGrpc.SecuredGreeterBlockingStub securedFutureStub = SecuredGreeterGrpc.newBlockingStub(selectedChanel);

        assertThrows(StatusRuntimeException.class, () ->
            securedFutureStub.sayAuthHello(Empty.getDefaultInstance()).getMessage()
        );

    }

    @Override
    protected GreeterGrpc.GreeterFutureStub beforeGreeting(GreeterGrpc.GreeterFutureStub stub) {
        return stub.withCallCredentials(callCredentials);
    }
}
