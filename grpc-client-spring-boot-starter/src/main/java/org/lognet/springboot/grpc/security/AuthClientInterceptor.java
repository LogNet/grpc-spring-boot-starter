package org.lognet.springboot.grpc.security;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import lombok.Builder;

import java.util.function.Supplier;

/**
 * Adds Authorization header with configured authentication scheme token supplied by tokeSupplier to each intercepted client call
 */

@Builder
public class AuthClientInterceptor implements ClientInterceptor {
    public static class AuthClientInterceptorBuilder {

        public AuthClientInterceptorBuilder bearer() {
            return authScheme(Constants.BEARER_AUTH_SCHEME);
        }
        public AuthClientInterceptorBuilder basic() {
            return  authScheme(Constants.BASIC_AUTH_SCHEME);
        }
    }

    private final Supplier<String> tokenSupplier;
    private final String authScheme;


    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel next) {
        return new ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT>(next.newCall(methodDescriptor, callOptions)) {
            @Override
            protected void checkedStart(Listener<RespT> responseListener, Metadata headers) throws Exception {

                headers.put(Constants.AUTH_HEADER_KEY, String.format("%s %s",authScheme, tokenSupplier.get()));
                delegate().start(responseListener, headers);
            }
        };
    }
}
