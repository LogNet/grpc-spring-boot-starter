package org.lognet.springboot.grpc.autoconfigure.actuate;

import io.grpc.BindableService;
import io.grpc.ServiceDescriptor;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import lombok.Builder;
import lombok.Getter;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.GRpcServicesRegistry;
import org.lognet.springboot.grpc.autoconfigure.GRpcAutoConfiguration;
import org.lognet.springboot.grpc.autoconfigure.OnGrpcServerEnabled;
import org.lognet.springboot.grpc.context.GRpcServerInitializedEvent;
import org.lognet.springboot.grpc.context.LocalRunningGrpcPort;
import org.lognet.springboot.grpc.health.ManagedHealthStatusService;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ConditionalOnClass(HealthContributor.class)
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(GRpcAutoConfiguration.class)
@ConditionalOnBean(annotation = GRpcService.class)
@OnGrpcServerEnabled
public class GRpcActuateAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnEnabledHealthIndicator("grpc")
    static class GRpcHealthHealthContributorConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = "grpcHealthIndicator")
        public HealthContributor grpcHealthIndicator(GRpcServicesRegistry registry, ManagedHealthStatusService healthStatusService) {
            final Map<String, HealthIndicator> services = registry.getServiceNameToServiceBeanMap()
                    .keySet()
                    .stream()
                    .filter(s -> !HealthGrpc.SERVICE_NAME.equals(s))
                    .collect(Collectors.toMap(Function.identity(), s -> new AbstractHealthIndicator() {

                        @Override
                        protected void doHealthCheck(Health.Builder builder) throws Exception {
                            final HealthCheckResponse.ServingStatus status = healthStatusService.statuses().get(s);
                            if (null == status) {
                                builder.unknown();
                                return;
                            }
                            switch (status) {
                                case SERVING:
                                    builder.up();
                                    break;
                                case NOT_SERVING:
                                    builder.down();
                                    break;
                                case UNKNOWN:
                                case UNRECOGNIZED:
                                    builder.unknown();
                                    break;
                                case SERVICE_UNKNOWN:
                                    builder.outOfService();
                                    break;

                            }
                        }
                    }));
            return CompositeHealthContributor.fromMap(services);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnAvailableEndpoint(endpoint = GrpcEndpoint.class)
    static class GrpcEndpointConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public GrpcEndpoint grpcEndpoint(GRpcServicesRegistry registry) {
            return new GrpcEndpoint(registry);
        }

    }

    @Endpoint(id = "grpc")
    static class GrpcEndpoint {
        public static final class GRpcServices {

            @Builder
            @Getter
            static class GrpcService{
                private String name;
                private List<GrpcMethod> methods;
            }
            @Builder
            @Getter
            static class GrpcMethod{
                private String name;
            }
            @Getter
            private final List<GrpcService>  services;

            @Getter
            int port;

            private GRpcServices(Map<String, BindableService> services, int port) {
                this.port = port;

                this.services =  services
                        .values()
                        .stream()
                        .map(
                                s -> {
                                    final ServiceDescriptor serviceDescriptor = s.bindService().getServiceDescriptor();
                                    return GrpcService.builder()
                                            .name(serviceDescriptor.getName())
                                            .methods(serviceDescriptor.getMethods()
                                                    .stream()
                                                    .map(m->GrpcMethod.builder()
                                                            .name(m.getBareMethodName())
                                                            .build()
                                                    )
                                                    .collect(Collectors.toList())
                                            )
                                            .build();


                                }
                        ).collect(Collectors.toList());
            }
        }


        private final GRpcServicesRegistry registry;
        private int port;

        public GrpcEndpoint(GRpcServicesRegistry registry ) {
            this.registry = registry;

        }


        @EventListener
        public void onGrpcServerStarted(GRpcServerInitializedEvent e){
            port = e.getServer().getPort();
        }


        @ReadOperation
        public GRpcServices services() {
            return new GRpcServices(registry.getBeanNameToServiceBeanMap(),port);
        }
    }


}
