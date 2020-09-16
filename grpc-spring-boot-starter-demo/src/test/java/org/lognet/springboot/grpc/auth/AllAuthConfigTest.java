package org.lognet.springboot.grpc.auth;


import com.google.protobuf.Empty;
import io.grpc.examples.SecuredGreeterGrpc;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.security.EnableGrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.lognet.springboot.grpc.security.jwt.JwtAuthProviderFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@SpringBootTest(classes = DemoApp.class)
@ActiveProfiles("keycloack-test")
@RunWith(SpringRunner.class)
@Import({AllAuthConfigTest.TestCfg.class})
public class AllAuthConfigTest extends JwtAuthBaseTest {

    @TestConfiguration
    @EnableGrpcSecurity
    static class TestCfg  extends GrpcSecurityConfigurerAdapter {
        @Override
        public void configure(GrpcSecurity builder) throws Exception {
            builder.authorizeRequests()
                    .anyMethod().authenticated()
            .and()
            .authenticationProvider(JwtAuthProviderFactory.withAuthorities(getContext().getBean(JwtDecoder.class)));
        }
    }

    @Test
    public void securedServiceTest() {

        final SecuredGreeterGrpc.SecuredGreeterBlockingStub   securedFutureStub = SecuredGreeterGrpc.newBlockingStub(selectedChanel);

        final String reply = securedFutureStub.sayAuthHello(Empty.getDefaultInstance()).getMessage();
        assertNotNull("Reply should not be null",reply);
        assertTrue(String.format("Reply should contain name '%s'",USER_NAME),reply.contains(USER_NAME));

    }
}
