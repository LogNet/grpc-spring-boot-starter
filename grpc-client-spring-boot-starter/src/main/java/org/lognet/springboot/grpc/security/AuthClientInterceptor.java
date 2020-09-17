package org.lognet.springboot.grpc.security;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Adds Authorization header with configured authentication scheme token supplied by tokeSupplier to each intercepted client call
 */


public class AuthClientInterceptor implements ClientInterceptor {
    private AuthHeader authHeader;

    public AuthClientInterceptor(AuthHeader authHeader) {
        this.authHeader = authHeader;
    }
    public AuthClientInterceptor(AuthHeader.AuthHeaderBuilder authHeaderBuilder) {
        this(authHeaderBuilder.build());
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel next) {
        return new ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT>(next.newCall(methodDescriptor, callOptions)) {
            @Override
            protected void checkedStart(Listener<RespT> responseListener, Metadata headers) throws Exception {
                delegate().start(responseListener, authHeader.attach(headers));
            }
        };
    }
}
