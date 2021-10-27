package org.lognet.springboot.grpc.auth;


import com.google.protobuf.Empty;
import io.grpc.examples.GreeterGrpc;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.MatcherAssert.assertThat;


@SpringBootTest(classes = DemoApp.class)
@RunWith(SpringRunner.class)
@Import({DisabledSecuredAnnTest.TestCfg.class})
public class DisabledSecuredAnnTest extends JwtAuthBaseTest {


    @TestConfiguration
    static class TestCfg  extends GrpcSecurityConfigurerAdapter {
        @Override
        public void configure(GrpcSecurity builder) throws Exception {
            builder.authorizeRequests().withoutSecuredAnnotation();
        }
    }

    public DisabledSecuredAnnTest() {
        super(false);
    }

    @Test
    public void nonSecuredServiceTest() {

        final GreeterGrpc.GreeterBlockingStub   greeter = GreeterGrpc.newBlockingStub(getChannel(true));

        final String reply = greeter.sayAuthHello(Empty.getDefaultInstance()).getMessage();
        assertThat(reply, Matchers.notNullValue(String.class));
        assertThat(reply,Matchers.not("anonymous"));


    }
}
