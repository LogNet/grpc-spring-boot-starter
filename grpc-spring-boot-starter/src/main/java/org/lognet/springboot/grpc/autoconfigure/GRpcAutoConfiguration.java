package org.lognet.springboot.grpc.autoconfigure;

import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.lognet.springboot.grpc.FailureHandlingSupport;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.lognet.springboot.grpc.GRpcServerBuilderConfigurer;
import org.lognet.springboot.grpc.GRpcServerRunner;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.GRpcServicesRegistry;
import org.lognet.springboot.grpc.health.DefaultHealthStatusService;
import org.lognet.springboot.grpc.recovery.GRpcExceptionHandlerInterceptor;
import org.lognet.springboot.grpc.recovery.GRpcExceptionHandlerMethodResolver;
import org.lognet.springboot.grpc.recovery.GRpcServiceAdvice;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindHandlerAdvisor;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.SocketUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Created by alexf on 25-Jan-16.
 */

@AutoConfigureOrder
@AutoConfigureAfter(ValidationAutoConfiguration.class)
@ConditionalOnBean(annotation = GRpcService.class)
@Import({GRpcValidationConfiguration.class,
        NettyServerBuilderSelector.class,
        DefaultHealthStatusService.class
})
@Configuration
public class GRpcAutoConfiguration {

    @Bean
    @OnGrpcServerEnabled
    public GRpcServerRunner grpcServerRunner(@Qualifier("grpcInternalConfigurator") Consumer<ServerBuilder<?>> configurator, ServerBuilder<?> serverBuilder) {
        return new GRpcServerRunner(configurator, serverBuilder);
    }

    @Bean
    @ConditionalOnProperty(prefix = "grpc", name = "inProcessServerName")
    public GRpcServerRunner grpcInprocessServerRunner(@Qualifier("grpcInternalConfigurator") Consumer<ServerBuilder<?>> configurator,GRpcServerProperties gRpcServerProperties) {
        return new GRpcServerRunner(configurator, InProcessServerBuilder.forName(gRpcServerProperties.getInProcessServerName()));
    }

    @Bean
    public GRpcServicesRegistry grpcServicesRegistry() {
        return new GRpcServicesRegistry();
    }

    @Bean
    public GRpcExceptionHandlerMethodResolver exceptionHandlerMethodResolver(GRpcServicesRegistry gRpcServicesRegistry, ApplicationContext applicationContext){
        final Collection<Object> advices = applicationContext.getBeansWithAnnotation(GRpcServiceAdvice.class).values();
        return new GRpcExceptionHandlerMethodResolver(gRpcServicesRegistry, advices);
    }

    @Bean
    public FailureHandlingSupport failureHandlingSupport(GRpcExceptionHandlerMethodResolver methodResolver){
        return  new FailureHandlingSupport(methodResolver);
    }

    @Bean
    @GRpcGlobalInterceptor
    public GRpcExceptionHandlerInterceptor exceptionHandlerInterceptor(FailureHandlingSupport failureHandlingSupport,
                                                                       GRpcExceptionHandlerMethodResolver methodResolver,
                                                                       GRpcServerProperties serverProperties) {
        return new GRpcExceptionHandlerInterceptor(methodResolver,failureHandlingSupport,serverProperties);
    }

    @Bean
    @ConditionalOnMissingBean(GRpcServerBuilderConfigurer.class)
    public GRpcServerBuilderConfigurer serverBuilderConfigurer() {
        return new GRpcServerBuilderConfigurer();
    }

    @Bean
    public GRpcServerProperties gRpcServerProperties(){
        return  new GRpcServerProperties();
    }

    @ConditionalOnMissingClass("org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties")
    @Bean
    public ConfigurationPropertiesBindHandlerAdvisor skipConsulDiscoveryBinding( ){
        return  bindHandler-> new AbstractBindHandler(bindHandler) {
            private final ConfigurationPropertyName grpcConsulConfigProperty = ConfigurationPropertyName.of("grpc.consul");
            @Override
            public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
                // otherwise, it will try to instantiate grpc.consul property and discovery field class doesn't exist
                return grpcConsulConfigProperty.equals(name)? null : super.onStart(name, target, context);
            }
        } ;
    }


    @Bean(name = "grpcInternalConfigurator")
    public Consumer<ServerBuilder<?>> configurator(List<GRpcServerBuilderConfigurer> configurers, GRpcServerProperties grpcServerProperties) {
        return serverBuilder -> {
            if (grpcServerProperties.isEnabled()) {
                Optional.ofNullable(grpcServerProperties.getSecurity())
                        .ifPresent(s -> {
                            boolean setupSecurity = Optional.ofNullable(s.getCertChain()).isPresent();
                            if (setupSecurity != Optional.ofNullable(s.getPrivateKey()).isPresent()) {
                                throw new BeanCreationException("Both  gRPC  TLS 'certChain' and 'privateKey' should be configured. One of them is null. ");
                            }
                            if (setupSecurity) {
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
            configurers.forEach(c->c.configure(serverBuilder));
        };
    }

    @Bean
    @ConfigurationPropertiesBinding
    public static Converter<String, InetSocketAddress> socketAddressConverter() {
        return new Converter<String, InetSocketAddress>() {
            @Override
            public InetSocketAddress convert(String source) {
                final String[] chunks = source.split(":");
                int port;
                switch (chunks.length) {
                    case 1:
                        port = GRpcServerProperties.DEFAULT_GRPC_PORT;
                        break;
                    case 2:
                        port = Integer.parseInt(chunks[1]);
                        if (port < 1) {
                            port = SocketUtils.findAvailableTcpPort();
                        }
                        break;
                    default:
                        throw new IllegalArgumentException(source + " can't be converted to socket address");

                }

                return new InetSocketAddress(chunks[0], port);
            }
        };
    }


}
