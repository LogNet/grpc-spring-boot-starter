package org.lognet.springboot.grpc.tracing;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

public class TracingServerInterceptor implements ServerInterceptor {

    private final Tracer tracer;

    public TracingServerInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        String traceId = headers.get(Metadata.Key.of("traceId", Metadata.ASCII_STRING_MARSHALLER));
        String spanId = headers.get(Metadata.Key.of("spanId", Metadata.ASCII_STRING_MARSHALLER));

        Context spanContext = createSpanContext(traceId, spanId);
        SpanBuilder spanBuilder = tracer.spanBuilder("grpc-spring-boot-starter-span").setParent(spanContext);
        Span span = spanBuilder.startSpan();
        Context scopedContext = Context.current().with(span);

        try (Scope scope = scopedContext.makeCurrent()) {
            return next.startCall(call, headers);
        } finally {
            span.end();
        }
    }

    private Context createSpanContext(String traceId, String spanId) {
        SpanContext spanContext = SpanContext.createFromRemoteParent(
                traceId != null ? traceId : "",
                spanId != null ? spanId : "",
                TraceFlags.getDefault(),
                TraceState.getDefault()
        );
        return Context.current().with(Span.wrap(spanContext));
    }
}
