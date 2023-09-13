package org.lognet.springboot.grpc.autoconfigure.tracing;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.lognet.springboot.grpc.tracing.TracingServerInterceptor;
import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({OpenTelemetrySdk.class})
@EnableConfigurationProperties(OtlpProperties.class)
public class OtelConfiguration {

    @Bean
    public OtlpGrpcSpanExporter otlpExporter(OtlpProperties properties) {
        OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder().setEndpoint(properties.getEndpoint());
        return builder.build();
    }

    @Bean
    public Tracer tracer(OtlpGrpcSpanExporter otlpExporter) {
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(otlpExporter).build())
                .build();

        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();

        return openTelemetry.getTracerProvider().get("grpc-spring-boot-starter");
    }

    @Bean
    public TracingServerInterceptor tracingServerInterceptor(Tracer tracer) {
        return new TracingServerInterceptor(tracer);
    }
}
