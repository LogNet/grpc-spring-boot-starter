package org.lognet.springboot.grpc.autoconfigure.metrics;

import static java.util.stream.Collectors.toList;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.lognet.springboot.grpc.GRpcServerRunner;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.autoconfigure.GRpcAutoConfiguration;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@AutoConfigureAfter({MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class, GRpcAutoConfiguration.class})
@ConditionalOnClass({MeterRegistry.class})
@Conditional(GRpcMetricsAutoConfiguration.OnGrpcAndMeterRegistryEnabledCondition.class)
@ConditionalOnBean(GRpcServerRunner.class)
@EnableConfigurationProperties(GRpcMetricsProperties.class)
public class GRpcMetricsAutoConfiguration {

    protected static class OnGrpcAndMeterRegistryEnabledCondition extends AllNestedConditions {

        OnGrpcAndMeterRegistryEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnBean(value = {MeterRegistry.class})
        static class MeterRegistryCondition {
        }

        @ConditionalOnBean(annotation = {GRpcService.class})
        static class GrpcServiceCondition {
        }

    }

    static class MonitoringServerCall<ReqT, RespT> extends ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {
        private final MeterRegistry registry;
        final Timer.Sample start;
        private final Collection<GRpcMetricsTagsContributor> tagsContributors;
        @Getter
        private Tags additionalTags = Tags.empty();
        private volatile boolean closed = false;

        protected MonitoringServerCall(ServerCall<ReqT, RespT> delegate, MeterRegistry registry, Collection<GRpcMetricsTagsContributor> tagsContributors) {
            super(delegate);
            this.start = Timer.start(registry);
            this.registry = registry;
            this.tagsContributors = tagsContributors;
        }

        @Override
        public void close(Status status, Metadata trailers) {
            if (!closed) { //close is called twice , first time with actual status
                closed = true;
                final Timer.Builder timerBuilder = Timer.builder("grpc.server.calls");
                tagsContributors.forEach(c ->
                    timerBuilder.tags(c.getTags(status, getMethodDescriptor(), getAttributes()))
                );
                Optional.ofNullable(additionalTags)
                    .ifPresent(timerBuilder::tags);
                start.stop(timerBuilder.register(registry));
            }
            super.close(status, trailers);
        }

        public void addTags(Iterable<Tag> tags) {
            additionalTags = additionalTags.and(tags);
        }
    }

    @Slf4j
    private static class MessageMonitoringListener<ReqT> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {
        private final MonitoringServerCall<ReqT, ?> monitoringServerCall;
        private final Collection<RequestAwareGRpcMetricsTagsContributor<?>> requestAwareContributorCandidates;

        protected MessageMonitoringListener(
            ServerCall.Listener<ReqT> delegate,
            MonitoringServerCall<ReqT, ?> monitoringServerCall,
            Collection<RequestAwareGRpcMetricsTagsContributor<?>> requestAwareContributorCandidates
        ) {
            super(delegate);
            this.monitoringServerCall = monitoringServerCall;
            this.requestAwareContributorCandidates = requestAwareContributorCandidates;
        }

        @Override
        public void onMessage(ReqT message) {
            for (RequestAwareGRpcMetricsTagsContributor<?> contributor : requestAwareContributorCandidates) {
                if (contributor.accepts(message)) {
                    try {
                        //noinspection unchecked
                        monitoringServerCall.addTags(
                            ((RequestAwareGRpcMetricsTagsContributor<ReqT>) contributor).addTags(
                                message,
                                monitoringServerCall.getMethodDescriptor(),
                                monitoringServerCall.getAttributes(),
                                monitoringServerCall.getAdditionalTags()
                            )
                        );
                    } catch (RuntimeException error) {
                        log.error("Failed to  execute tag contributor {}", contributor, error);
                    }
                }
            }
            super.onMessage(message);
        }
    }

    static class MonitoringServerInterceptor implements ServerInterceptor, Ordered {
        private final MeterRegistry registry;

        private Collection<GRpcMetricsTagsContributor> tagsContributors ;
        private Collection<RequestAwareGRpcMetricsTagsContributor<?>> requestAwareGRpcMetricsTagsContributors;

        @Autowired
        public void setTagsContributors(Collection<GRpcMetricsTagsContributor> tagsContributors) {
            this.tagsContributors = tagsContributors;
            this.requestAwareGRpcMetricsTagsContributors = tagsContributors.stream()
                .filter(RequestAwareGRpcMetricsTagsContributor.class::isInstance)
                .map(contributor -> (RequestAwareGRpcMetricsTagsContributor<?>) contributor)
                .collect(toList());
        }

        @Setter
        @Accessors(fluent = true)
        private Integer order;

        public MonitoringServerInterceptor(MeterRegistry registry) {
            this.registry = registry;
        }

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

            final MonitoringServerCall<ReqT, RespT> monitoringServerCall = new MonitoringServerCall<>(call, registry,tagsContributors);
            final ServerCall.Listener<ReqT> measuredCall = next.startCall(monitoringServerCall, headers);
            List<RequestAwareGRpcMetricsTagsContributor<?>> requestAwareContributorCandidates =
                requestAwareGRpcMetricsTagsContributors.stream()
                    .filter(contributor -> contributor.mightAccept(call.getMethodDescriptor()))
                    .collect(toList());
            if (!requestAwareContributorCandidates.isEmpty()) {
                return new MessageMonitoringListener<>(measuredCall, monitoringServerCall, requestAwareContributorCandidates);
            } else {
                return measuredCall;
            }
        }

        @Override
        public int getOrder() {
            return Optional.ofNullable(order).orElse(HIGHEST_PRECEDENCE + 20);
        }
    }

    @Bean
    @GRpcGlobalInterceptor
    public ServerInterceptor measure(MeterRegistry registry, GRpcMetricsProperties metricsProperties) {

        return new MonitoringServerInterceptor(registry)
                .order(metricsProperties.getInterceptorOrder());
    }

    @Bean
    public GRpcMetricsTagsContributor defaultTagsContributor(GRpcServerProperties properties) {
        final boolean hasMultipleAddresses = Optional.ofNullable(properties.getNettyServer())
            .map(GRpcServerProperties.NettyServerProperties::getAdditionalListenAddresses)
            .map(l -> !l.isEmpty())
            .orElse(false);

        GRpcMetricsTagsContributor defaultContributor = (status, methodDescriptor, attributes) -> Tags.of(
            "result", status.getCode().name(),
            "method", methodDescriptor.getFullMethodName()
        );

        if (hasMultipleAddresses) {
            return (status, methodDescriptor, attributes) -> {
                String address = Optional.ofNullable(attributes)
                    .map(a -> a.get(Grpc.TRANSPORT_ATTR_LOCAL_ADDR))
                    .map(SocketAddress::toString)
                    .orElse("");
                return Tags.concat(
                    defaultContributor.getTags(status, methodDescriptor, attributes),
                    "address", address
                );
            };
        } else {
            return defaultContributor;
        }
    }
}
