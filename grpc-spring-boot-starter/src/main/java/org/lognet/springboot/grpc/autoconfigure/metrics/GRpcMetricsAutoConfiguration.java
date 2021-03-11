package org.lognet.springboot.grpc.autoconfigure.metrics;

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
import io.micrometer.core.instrument.Timer;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.lognet.springboot.grpc.GRpcService;
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

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Configuration
@AutoConfigureAfter({MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@ConditionalOnClass({MeterRegistry.class})
@Conditional(GRpcMetricsAutoConfiguration.OnGrpcAndMeterRegistryEnabledCondition.class)
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


        private MeterRegistry registry;

        final Timer.Sample start;

        private Collection<GRpcMetricsTagsContributor> tagsContributors;
        private  List<Tag> additionalTags;



        protected MonitoringServerCall(ServerCall<ReqT, RespT> delegate, MeterRegistry registry, Collection<GRpcMetricsTagsContributor> tagsContributors) {
            super(delegate);
            this.start = Timer.start(registry);
            this.registry = registry;
            this.tagsContributors = tagsContributors;
        }

        @Override
        public void close(Status status, Metadata trailers) {

            final Timer.Builder timerBuilder = Timer.builder("grpc.server.calls");
            tagsContributors.forEach(c->
                timerBuilder.tags(c.getTags(status,getMethodDescriptor(),getAttributes()))
            );
            Optional.ofNullable(additionalTags)
                    .ifPresent(timerBuilder::tags);

            start.stop(timerBuilder.register(registry));

            super.close(status, trailers);
        }

        public void addTags(List<Tag> tags) {
            additionalTags  = tags;
        }
    }

    @Slf4j
    static class MonitoringServerInterceptor implements ServerInterceptor, Ordered {

        private MeterRegistry registry;



        private Collection<GRpcMetricsTagsContributor> tagsContributors ;

        @Autowired
        public void setTagsContributors(Collection<GRpcMetricsTagsContributor> tagsContributors) {
            this.tagsContributors = tagsContributors;
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
            if (call.getMethodDescriptor().getType().clientSendsOneMessage()) {
                return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(measuredCall) {
                    @Override
                    public void onMessage(ReqT message) {

                            final Stream<Tag> fd = tagsContributors
                                    .stream()
                                    .filter(RequestAwareGRpcMetricsTagsContributor.class::isInstance)
                                    .map(RequestAwareGRpcMetricsTagsContributor.class::cast)
                                    .filter(c -> c.accepts(message))
                                    .flatMap(c -> {
                                        try {
                                            return StreamSupport.stream(c.getTags(message,monitoringServerCall.getMethodDescriptor(),monitoringServerCall.getAttributes()).spliterator(), false);
                                        }catch (Throwable t){
                                            log.error("Failed to  execute tag contributor",t);
                                            return Stream.empty();
                                        }
                                    });

                            monitoringServerCall.addTags(fd.collect(Collectors.toList()));

                        super.onMessage(message);

                    }
                };
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
        final Boolean hasMultipleAddresses = Optional.ofNullable(properties.getNettyServer())
                .map(GRpcServerProperties.NettyServerProperties::getAdditionalListenAddresses)
                .map(l -> !l.isEmpty())
                .orElse(false);

        return (status, methodDescriptor, attributes) -> {

            final ArrayList<Tag> tags = new ArrayList<>();
            tags.add(Tag.of("result", status.getCode().name()));
            tags.add(Tag.of("method", methodDescriptor.getFullMethodName()));
            if (hasMultipleAddresses) {
                Optional.ofNullable(attributes)
                        .map(a -> a.get(Grpc.TRANSPORT_ATTR_LOCAL_ADDR))
                        .map(SocketAddress::toString)
                        .map(a -> Tag.of("address", a))
                        .ifPresent(tags::add);
            }
            return tags;
        };
    }
}
