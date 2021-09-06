package org.lognet.springboot.grpc.auth;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.SecuredGreeterGrpc;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.HalfCloseInterceptor;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(
    classes = DemoApp.class,
    properties = "grpc.security.auth.fail-fast=false"
)
@RunWith(SpringRunner.class)
public class FailLateSecurityInterceptorTest extends GrpcServerTestBase {
    @SpyBean
    HalfCloseInterceptor halfCloseInterceptor;

    @Test
    public void noHalfCloseOnFailedAuth() {
        final StatusRuntimeException statusRuntimeException = assertThrows(
            StatusRuntimeException.class,
            () -> SecuredGreeterGrpc.newBlockingStub(selectedChanel).sayAuthHello2(Empty.newBuilder().build()).getMessage()
        );
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.UNAUTHENTICATED));
        verify(halfCloseInterceptor, never()).onHalfClose();
    }
}
