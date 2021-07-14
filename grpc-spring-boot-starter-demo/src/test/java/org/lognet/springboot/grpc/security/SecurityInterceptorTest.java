package org.lognet.springboot.grpc.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.custom.Custom.CustomReply;
import io.grpc.examples.custom.Custom.CustomRequest;
import io.grpc.examples.custom.CustomServiceGrpc;
import org.junit.Test;
import org.lognet.springboot.grpc.GRpcErrorHandler;

public class SecurityInterceptorTest {

    @SuppressWarnings("unchecked")
    @Test
    public void authSchemeSelectionException() {
        AuthenticationSchemeSelector throwingSchemeSelector = mock(AuthenticationSchemeSelector.class);
        Exception testException = new IllegalStateException("test");
        when(throwingSchemeSelector.getAuthScheme(any())).thenThrow(testException);
        GRpcErrorHandler errorHandler = spy(new GRpcErrorHandler());

        SecurityInterceptor securityInterceptor =
            new SecurityInterceptor(mock(GrpcSecurityMetadataSource.class), throwingSchemeSelector);
        securityInterceptor.setErrorHandler(Optional.of(errorHandler));
        securityInterceptor.setConfig(null);

        Metadata headers = new Metadata();
        headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "test");

        assertThrows(
            StatusRuntimeException.class,
            () -> securityInterceptor.interceptCall(mock(ServerCall.class), headers, (c, h) -> null)
        );
        verify(errorHandler).handle(eq(Status.UNAUTHENTICATED), same(testException), same(headers), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void innerInterceptorException() {
        GRpcErrorHandler errorHandler = spy(new GRpcErrorHandler());
        SecurityInterceptor securityInterceptor =
            new SecurityInterceptor(mock(GrpcSecurityMetadataSource.class), mock(AuthenticationSchemeSelector.class));
        securityInterceptor.setErrorHandler(Optional.of(errorHandler));
        securityInterceptor.setConfig(null);
        Metadata headers = new Metadata();

        ServerCallHandler<CustomRequest, CustomReply> throwingInnerInterceptor =
            (c, h) -> {throw new IllegalStateException("inner exception");};
        ServerCall<CustomRequest, CustomReply> call = mock(ServerCall.class);
        when(call.getMethodDescriptor()).thenReturn(CustomServiceGrpc.getCustomMethod());

        assertThrows(
            IllegalStateException.class,
            () -> securityInterceptor.interceptCall(call, headers, throwingInnerInterceptor)
        );
        verifyZeroInteractions(errorHandler);
    }
}
