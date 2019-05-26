package org.lognet.springboot.grpc.autoconfigure.consul;

import org.lognet.springboot.grpc.GRpcServerRunner;
import org.lognet.springboot.grpc.autoconfigure.GRpcAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistry;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter({ ConsulServiceRegistryAutoConfiguration.class, GRpcAutoConfiguration.class})
public class ConsulGrpcAutoConfiguration{


    @ConditionalOnProperty(value = "spring.cloud.service-registry.auto-registration.enabled", matchIfMissing = true)
    @ConditionalOnBean({ConsulServiceRegistry.class, GRpcServerRunner.class})
    @Bean
    public GrpcConsulRegistrar consulRegistrar(ConsulServiceRegistry consulServiceRegistry){
        return new GrpcConsulRegistrar(consulServiceRegistry);
    }


}

