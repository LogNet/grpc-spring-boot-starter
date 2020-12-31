package org.lognet.springboot.grpc.autoconfigure.metrics;

import io.grpc.ForwardingServerCall;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.net.SocketAddress;
import java.util.Optional;

@Configuration
@AutoConfigureAfter({MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@ConditionalOnClass({MeterRegistry.class})
@Conditional(GRpcMetricsAutoConfiguration.OnGrpcAndMeterRegistryEnabledCondition.class)
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

    static class MonitoringServerCall<ReqT, RespT> extends  ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {

        private final boolean addAddressTag;
        private MeterRegistry registry;
        final Timer.Sample start;
        protected MonitoringServerCall(ServerCall<ReqT, RespT> delegate, MeterRegistry registry, boolean addAddressTag) {
            super(delegate);
            this.start = Timer.start(registry);
            this.registry = registry;
            this.addAddressTag = addAddressTag;


        }
        @Override
        public void close(Status status, Metadata trailers) {
            final Timer.Builder timerBuilder = Timer.builder("grpc.server.calls")
                    .tag("method", getMethodDescriptor().getFullMethodName())
                    .tag("result", status.getCode().name());
            if (addAddressTag){
                Optional.ofNullable(delegate().getAttributes().get(Grpc.TRANSPORT_ATTR_LOCAL_ADDR))
                        .map(SocketAddress::toString)
                        .ifPresent(a->timerBuilder.tag("address",a));
            }
            start.stop(timerBuilder.register(registry));

            super.close(status, trailers);
        }
    }

    static class MonitoringServerInterceptor implements ServerInterceptor, Ordered {


        private MeterRegistry registry;
        private boolean addAddressTag;


        public MonitoringServerInterceptor(MeterRegistry registry,boolean addAddressTag) {
            this.registry = registry;
            this.addAddressTag = addAddressTag;
        }

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
            return next.startCall(new MonitoringServerCall<>(call,registry,addAddressTag), headers);

        }

        @Override
        public int getOrder() {
            return HIGHEST_PRECEDENCE+20;
        }
    }

    @Bean
    @GRpcGlobalInterceptor
    public ServerInterceptor measure(MeterRegistry registry, GRpcServerProperties properties){
        final Boolean hasMultipleAddresses = Optional.ofNullable(properties.getNettyServer())
                .map(GRpcServerProperties.NettyServerProperties::getAdditionalListenAddresses)
                .map(l -> !l.isEmpty())
                .orElse(false);
        return  new MonitoringServerInterceptor(registry,hasMultipleAddresses);
    }
}
