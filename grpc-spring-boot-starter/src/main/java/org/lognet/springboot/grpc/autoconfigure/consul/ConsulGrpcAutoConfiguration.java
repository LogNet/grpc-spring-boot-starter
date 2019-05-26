package org.lognet.springboot.grpc.autoconfigure.consul;

import org.lognet.springboot.grpc.GRpcServerRunner;
import org.lognet.springboot.grpc.autoconfigure.GRpcAutoConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistry;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter({ ConsulServiceRegistryAutoConfiguration.class, GRpcAutoConfiguration.class})
@ConditionalOnProperty(value = "spring.cloud.service-registry.auto-registration.enabled", matchIfMissing = true)
@ConditionalOnBean({ConsulServiceRegistry.class, GRpcServerRunner.class})

public class ConsulGrpcAutoConfiguration{


    @Bean
    public GrpcConsulRegistrar consulRegistrar(ServiceRegistry<ConsulRegistration> consulServiceRegistry){
        return new GrpcConsulRegistrar(consulServiceRegistry);
    }

   // @Bean
    public BeanPostProcessor consuleServiceRegistryPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if(ConsulServiceRegistry.class.isAssignableFrom(bean.getClass())){
                    return  new SmartCountingConsulServiceRegistry(ConsulServiceRegistry.class.cast(bean));
                }
                return  bean;
            }
        };
    }
}

