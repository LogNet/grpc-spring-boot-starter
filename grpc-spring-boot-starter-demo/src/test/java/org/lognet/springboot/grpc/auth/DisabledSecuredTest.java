package org.lognet.springboot.grpc.auth;


import com.google.protobuf.Empty;
import io.grpc.examples.SecuredGreeterGrpc;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.MatcherAssert.assertThat;


@SpringBootTest(classes = DemoApp.class,properties = "grpc.security.auth.enabled=false")
@RunWith(SpringRunner.class)

public class DisabledSecuredTest extends GrpcServerTestBase {


    @Test
    public void nonSecuredServiceTest() {

        final SecuredGreeterGrpc.SecuredGreeterBlockingStub greeter = SecuredGreeterGrpc.newBlockingStub(getChannel());

        final String reply = greeter.sayAuthHello2(Empty.getDefaultInstance()).getMessage();
        assertThat(reply, Matchers.notNullValue(String.class));
        assertThat(reply, Matchers.is("anonymous"));


    }
}
