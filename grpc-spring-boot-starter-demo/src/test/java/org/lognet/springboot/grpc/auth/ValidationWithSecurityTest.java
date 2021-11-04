package org.lognet.springboot.grpc.auth;


import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;


@SpringBootTest(classes = DemoApp.class
        ,properties = {
                "grpc.security.auth.fail-fast=false", // give validation interceptor a chance to validate message before authentication failure
                "grpc.security.auth.interceptor-order=4" // run after validator
        }
        )
@RunWith(SpringRunner.class)
@Import({ValidationWithSecurityTest.TestCfg.class})
public class ValidationWithSecurityTest extends GrpcServerTestBase {

    @TestConfiguration
    static class TestCfg{
        @Bean
        public FailedAuthGrpcSecurityConfig securityConfig(){
            return new FailedAuthGrpcSecurityConfig();
        }
    }

   @Test
   public void validationShouldInvokedBeforeAuthTest() {
        final GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(super.getChannel());
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> {
            stub.helloPersonValidResponse(GreeterOuterClass.Person.newBuilder()
                    .setAge(49)// valid
                    .clearName()//invalid
                    .build());
        });

        assertThat(e.getStatus().getCode(), Matchers.is(Status.Code.INVALID_ARGUMENT));


    }

    @Override
    public void simpleGreeting() throws Exception {
            // do nothing
    }
}
