package org.lognet.springboot.grpc.tracing;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.TraceId;

public class TracingClientInterceptor implements ClientInterceptor {
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(ClientCall.Listener<RespT> responseListener, Metadata headers) {
                Span currentSpan = Span.current();
                String traceId = currentSpan.getSpanContext().getTraceId();
                String spanId = currentSpan.getSpanContext().getSpanId();

                if (!traceId.equals(TraceId.getInvalid())) {
                    headers.put(Metadata.Key.of("traceId", Metadata.ASCII_STRING_MARSHALLER), traceId);
                    headers.put(Metadata.Key.of("spanId", Metadata.ASCII_STRING_MARSHALLER), spanId);
                }
                super.start(responseListener, headers);
            }
        };
    }
}
