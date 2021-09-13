package org.lognet.springboot.grpc;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

@GRpcGlobalInterceptor
public class HalfCloseInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next
    ) {
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(next.startCall(call, headers)) {
            @Override
            public void onHalfClose() {
                HalfCloseInterceptor.this.onHalfClose();
                super.onHalfClose();
            }
        };
    }

    public void onHalfClose() {}
}
