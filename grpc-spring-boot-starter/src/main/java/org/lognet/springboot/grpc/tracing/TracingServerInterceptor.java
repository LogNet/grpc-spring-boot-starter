package org.lognet.springboot.grpc.tracing;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.core.Ordered;

import java.util.Optional;

public class TracingServerInterceptor implements ServerInterceptor, Ordered {

    @Setter
    @Accessors(fluent = true)
    private Integer order;

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

    @Override
    public int getOrder() {
        return Optional.ofNullable(order).orElse(HIGHEST_PRECEDENCE);
    }
}
