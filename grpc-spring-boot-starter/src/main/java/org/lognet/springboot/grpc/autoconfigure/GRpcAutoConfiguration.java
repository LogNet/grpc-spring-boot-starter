package org.lognet.springboot.grpc.autoconfigure;

import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.lognet.springboot.grpc.GRpcServerBuilderConfigurer;
import org.lognet.springboot.grpc.GRpcServerRunner;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.health.DefaultHealthStatusService;
import org.lognet.springboot.grpc.health.ManagedHealthStatusService;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.SocketUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Created by alexf on 25-Jan-16.
 */

@AutoConfigureOrder
@AutoConfigureAfter(ValidationAutoConfiguration.class)
@ConditionalOnBean(annotation = GRpcService.class)
@EnableConfigurationProperties({GRpcServerProperties.class})
@Import({GRpcValidationConfiguration.class,
        NettyServerBuilderSelector.class
})
public class GRpcAutoConfiguration {


    @Autowired
    private GRpcServerProperties grpcServerProperties;

    @Bean
    @OnGrpcServerEnabled
    public GRpcServerRunner grpcServerRunner(@Qualifier("grpcInternalConfigurator") Consumer<ServerBuilder<?>> configurator,ServerBuilder<?> serverBuilder) {
        return new GRpcServerRunner(configurator, serverBuilder);
    }


    @Bean
    @ConditionalOnProperty(prefix = "grpc", name = "inProcessServerName")
    public GRpcServerRunner grpcInprocessServerRunner(@Qualifier("grpcInternalConfigurator") Consumer<ServerBuilder<?>> configurator) {
        return new GRpcServerRunner(configurator, InProcessServerBuilder.forName(grpcServerProperties.getInProcessServerName()));
    }

    @Bean
    @ConditionalOnMissingBean
    public ManagedHealthStatusService healthStatusManager() {
        return new DefaultHealthStatusService();
    }

    @Bean
    @ConditionalOnMissingBean(GRpcServerBuilderConfigurer.class)
    public GRpcServerBuilderConfigurer serverBuilderConfigurer() {
        return new GRpcServerBuilderConfigurer();
    }

    @Bean(name = "grpcInternalConfigurator")
    public Consumer<ServerBuilder<?>> configurator(GRpcServerBuilderConfigurer configurer) {
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
            configurer.configure(serverBuilder);
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
                        if(port<1){
                            port = SocketUtils.findAvailableTcpPort();
                        }
                        break;
                    default:
                        throw new IllegalArgumentException(source +" can't be converted to socket address");

                }

                return new InetSocketAddress(chunks[0], port);
            }
        };
    }




}
