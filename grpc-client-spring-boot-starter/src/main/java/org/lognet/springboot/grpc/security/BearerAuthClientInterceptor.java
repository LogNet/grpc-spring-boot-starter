package org.lognet.springboot.grpc.security;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import java.util.function.Supplier;

/**
 * Adds Authorization header with Bearer token supplied by tokeSupplier to each intercepted client call
 */
public class BearerAuthClientInterceptor implements ClientInterceptor {
    private Supplier<String> tokenSupplier;

    public BearerAuthClientInterceptor(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel next) {
        return new ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT>(next.newCall(methodDescriptor, callOptions)) {
            @Override
            protected void checkedStart(Listener<RespT> responseListener, Metadata headers) throws Exception {
                headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + tokenSupplier.get());
                delegate().start(responseListener, headers);
            }
        };
    }
}
