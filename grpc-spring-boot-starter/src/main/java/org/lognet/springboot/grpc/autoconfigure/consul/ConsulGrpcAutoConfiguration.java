package org.lognet.springboot.grpc.autoconfigure.consul;

import org.lognet.springboot.grpc.GRpcServerRunner;
import org.lognet.springboot.grpc.autoconfigure.GRpcAutoConfiguration;
import org.lognet.springboot.grpc.autoconfigure.consul.registry.GrpcCheckConsulRegistrar;
import org.lognet.springboot.grpc.autoconfigure.consul.registry.TcpCheckConsulRegistrar;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistry;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ConsulServiceRegistry.class)
@AutoConfigureAfter({ConsulServiceRegistryAutoConfiguration.class, GRpcAutoConfiguration.class})
@ConditionalOnProperty(value = "spring.cloud.service-registry.auto-registration.enabled", matchIfMissing = true)
@ConditionalOnBean({ConsulServiceRegistry.class, GRpcServerRunner.class})
public class ConsulGrpcAutoConfiguration {

    @Bean
    @ConditionalOnProperty(value = "grpc.health.endpoint-enable", havingValue = "false", matchIfMissing = true)
    public TcpCheckConsulRegistrar tcpCheckConsulRegistrar(ConsulServiceRegistry consulServiceRegistry) {
        return new TcpCheckConsulRegistrar(consulServiceRegistry);
    }

    @Bean
    @ConditionalOnProperty(value = "grpc.health.endpoint-enable", havingValue = "true")
    public GrpcCheckConsulRegistrar grpcCheckConsulRegistrar(ConsulServiceRegistry consulServiceRegistry) {
        return new GrpcCheckConsulRegistrar(consulServiceRegistry);
    }

}

