package org.lognet.springboot.grpc.auth;


import com.google.protobuf.Empty;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.SecuredGreeterGrpc;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@SpringBootTest(classes = DemoApp.class)
@ActiveProfiles("keycloack-test")
@RunWith(SpringRunner.class)
public class DefaultAuthConfigTest extends JwtAuthBaseTest {


    public DefaultAuthConfigTest() {
        super(false);
    }

    @Test
    public void securedServiceTest() {

        final SecuredGreeterGrpc.SecuredGreeterBlockingStub   securedFutureStub = SecuredGreeterGrpc.newBlockingStub(getChannel(true));

        final String reply = securedFutureStub.sayAuthHello(Empty.getDefaultInstance()).getMessage();
        assertNotNull("Reply should not be null",reply);
        assertTrue(String.format("Reply should contain name '%s'",USER_NAME),reply.contains(USER_NAME));

    }
    @Test
    public void securedServiceMethodTest() {

        final GreeterGrpc.GreeterBlockingStub   securedFutureStub = GreeterGrpc.newBlockingStub(getChannel(true));

        final String reply = securedFutureStub.sayAuthHello(Empty.getDefaultInstance()).getMessage();
        assertNotNull("Reply should not be null",reply);
        assertTrue(String.format("Reply should contain name '%s'",USER_NAME),reply.contains(USER_NAME));

    }
    @Test
    public void securedAuthOnlyServiceMethodTest() {

        final GreeterGrpc.GreeterBlockingStub   securedFutureStub = GreeterGrpc.newBlockingStub(getChannel(true));

        final String reply = securedFutureStub.sayAuthOnlyHello(Empty.getDefaultInstance()).getMessage();
        assertNotNull("Reply should not be null",reply);
        assertTrue(String.format("Reply should contain name '%s'",USER_NAME),reply.contains(USER_NAME));

    }
}
