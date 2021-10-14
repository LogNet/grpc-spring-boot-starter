package org.lognet.springboot.grpc.recovery;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

/**
 * Container object that provides exception-specific attributes.
 */
@Builder
@Getter
public class GRpcExceptionScope {
    private MethodDescriptor<?, ?> methodDescriptor;
    private Attributes methodCallAttributes;
    private Metadata callHeaders;
    private Object request;
    private Object response;
    private Object hint;
    /**
     * headers to send to the client
     */
    @Builder.Default
    final Metadata responseHeaders = new Metadata();

    public Object getRequestOrResponse(){
        return Optional.ofNullable(request).orElse(response);
    }

    public <T> Optional<T> getHintAs(Class<T> clazz){
        return Optional.ofNullable(hint)
                .filter(clazz::isInstance)
                .map(clazz::cast);
    }
}
