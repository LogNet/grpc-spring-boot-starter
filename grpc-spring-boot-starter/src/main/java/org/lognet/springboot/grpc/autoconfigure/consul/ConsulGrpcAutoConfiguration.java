package org.lognet.springboot.grpc.autoconfigure.consul;

import org.lognet.springboot.grpc.GRpcServerRunner;
import org.lognet.springboot.grpc.autoconfigure.GRpcAutoConfiguration;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.lognet.springboot.grpc.autoconfigure.OnGrpcServerEnabled;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindHandlerAdvisor;
import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistry;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ConsulServiceRegistry.class)
@AutoConfigureAfter({ConsulServiceRegistryAutoConfiguration.class, GRpcAutoConfiguration.class})
@ConditionalOnProperty(value = "spring.cloud.service-registry.auto-registration.enabled", matchIfMissing = true)
@ConditionalOnBean({ConsulServiceRegistry.class, GRpcServerRunner.class})
@OnGrpcServerEnabled
public class ConsulGrpcAutoConfiguration {




    @Bean
    public ConfigurationPropertiesBindHandlerAdvisor advisor( ){
        // sets cloud consul discovery bound instance as starting object for grpc consul discovery properties
        return  b->new AbstractBindHandler(b) {
            private final ConfigurationPropertyName grpcConfigName = ConfigurationPropertyName.of("grpc");
            @Override
            public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {

                if(grpcConfigName.equals(name)){

                    final ConsulDiscoveryProperties result = context.getBinder().bindOrCreate(ConsulDiscoveryProperties.PREFIX, ConsulDiscoveryProperties.class);
                    final  GRpcServerProperties p = (GRpcServerProperties) target.getValue().get();
                    p.getConsul().setDiscovery(result);
                }
                return super.onStart(name, target, context);
            }
        } ;
    }



    @Bean
    public GrpcConsulRegistrar consulRegistrar(ConsulServiceRegistry consulServiceRegistry){
        return new GrpcConsulRegistrar(consulServiceRegistry);
    }


}

