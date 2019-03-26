package org.lognet.springboot.grpc.autoconfigure;

import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.services.HealthStatusManager;
import org.lognet.springboot.grpc.GRpcServerBuilderConfigurer;
import org.lognet.springboot.grpc.GRpcServerRunner;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Created by alexf on 25-Jan-16.
 */

@AutoConfigureOrder
@ConditionalOnBean(annotation = GRpcService.class)
@EnableConfigurationProperties (GRpcServerProperties.class)
public class GRpcAutoConfiguration {


    @Autowired
    private GRpcServerProperties grpcServerProperties;

    @Bean
    @ConditionalOnProperty(value = "grpc.enabled", havingValue = "true", matchIfMissing = true)
    public GRpcServerRunner grpcServerRunner(GRpcServerBuilderConfigurer configurer) {
        return new GRpcServerRunner(configurer, ServerBuilder.forPort(grpcServerProperties.getRunningPort()));
    }

    @Bean
    @ConditionalOnExpression("#{environment.getProperty('grpc.inProcessServerName','')!=''}")
    public GRpcServerRunner grpcInprocessServerRunner(GRpcServerBuilderConfigurer configurer){
        return new GRpcServerRunner(configurer, InProcessServerBuilder.forName(grpcServerProperties.getInProcessServerName()));
    }



    @Bean
    public HealthStatusManager healthStatusManager() {
        return new HealthStatusManager();
    }

    @Bean
    @ConditionalOnMissingBean(  GRpcServerBuilderConfigurer.class)
    public GRpcServerBuilderConfigurer serverBuilderConfigurer(){
        return new GRpcServerBuilderConfigurer();
    }
}
