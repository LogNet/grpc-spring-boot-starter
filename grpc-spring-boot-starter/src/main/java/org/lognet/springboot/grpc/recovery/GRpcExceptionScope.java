package org.lognet.springboot.grpc.recovery;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GRpcExceptionScope {
    private MethodDescriptor<?, ?> methodDescriptor;
    private Attributes methodCallAttributes;
    private Metadata callHeaders;
    private Object request;
    final Metadata responseHeaders = new Metadata();
}
