package org.lognet.springboot.grpc.autoconfigure;

import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.services.HealthStatusManager;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcServerBuilderConfigurer;
import org.lognet.springboot.grpc.GRpcServerRunner;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Created by alexf on 25-Jan-16.
 */

@AutoConfigureOrder
@ConditionalOnBean(annotation = GRpcService.class)
@EnableConfigurationProperties (GRpcServerProperties.class)
@Slf4j
public class GRpcAutoConfiguration {

    @Autowired
    private GRpcServerProperties grpcServerProperties;

    @Bean
    @ConditionalOnProperty(value = "grpc.enabled", havingValue = "true", matchIfMissing = true)
    public GRpcServerRunner grpcServerRunner(@Qualifier("grpcInternalConfigurator") Consumer<ServerBuilder<?>> configurator) {
        return new GRpcServerRunner(configurator, ServerBuilder.forPort(grpcServerProperties.getRunningPort()));
    }

    @Bean
    @ConditionalOnExpression("#{environment.getProperty('grpc.inProcessServerName','')!=''}")
    public GRpcServerRunner grpcInprocessServerRunner(@Qualifier("grpcInternalConfigurator") Consumer<ServerBuilder<?>> configurator){
        return new GRpcServerRunner(configurator, InProcessServerBuilder.forName(grpcServerProperties.getInProcessServerName()));
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

    @Bean(name = "grpcInternalConfigurator")
    public Consumer<ServerBuilder<?>> configurator(GRpcServerBuilderConfigurer configurer){
        return serverBuilder -> {
            if(grpcServerProperties.isEnabled()){
                Optional.ofNullable(grpcServerProperties.getSecurity())
                        .ifPresent(s->{
                            boolean setupSecurity = Optional.ofNullable(s.getCertChain()).isPresent();
                            if(setupSecurity != Optional.ofNullable(s.getPrivateKey()).isPresent() ){
                                throw  new BeanCreationException("Both  gRPC  TLS 'certChain' and 'privateKey' should be configured. One of them is null. ");
                            }
                            if(setupSecurity) {
                                try {
                                    serverBuilder.useTransportSecurity(s.getCertChain().getInputStream(),
                                            s.getPrivateKey().getInputStream()
                                    );
                                } catch (IOException e) {
                                    throw new BeanCreationException("Failed to setup security", e);
                                }
                            }
                        });
            }
            configurer.configure(serverBuilder);
        };
    }

}
