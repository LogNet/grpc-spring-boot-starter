package org.lognet.springboot.grpc.autoconfigure;

import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnClass(NettyServerBuilder.class)
public class PureNettyConfiguration {

    @Bean
    @OnGrpcServerEnabled
    public ServerBuilder<?> nettyServerBuilder(GRpcServerProperties grpcServerProperties){
        return Optional.ofNullable(grpcServerProperties.getNettyServer())
                .<ServerBuilder<?>> map(n->{
                    final NettyServerBuilder builder = Optional.ofNullable(n.getPrimaryListenAddress())
                            .map(NettyServerBuilder::forAddress)
                            .orElse(NettyServerBuilder.forPort(grpcServerProperties.getRunningPort()));


                    Optional.ofNullable(n.getAdditionalListenAddresses())
                            .ifPresent(l->l.forEach(builder::addListenAddress));

                    Optional.ofNullable(n.getFlowControlWindow())
                            .ifPresent(builder::flowControlWindow);

                    Optional.ofNullable(n.getInitialFlowControlWindow())
                            .ifPresent(builder::initialFlowControlWindow);

                    Optional.ofNullable(n.getKeepAliveTime())
                            .ifPresent(t->builder.keepAliveTime(t.toMillis(), TimeUnit.MILLISECONDS));

                    Optional.ofNullable(n.getKeepAliveTimeout())
                            .ifPresent(t->builder.keepAliveTimeout(t.toMillis(), TimeUnit.MILLISECONDS));

                    Optional.ofNullable(n.getPermitKeepAliveTime())
                            .ifPresent(t->builder.permitKeepAliveTime(t.toMillis(), TimeUnit.MILLISECONDS));


                    Optional.ofNullable(n.getMaxConnectionAge())
                            .ifPresent(t->builder.maxConnectionAge(t.toMillis(), TimeUnit.MILLISECONDS));

                    Optional.ofNullable(n.getMaxConnectionAgeGrace())
                            .ifPresent(t->builder.maxConnectionAgeGrace(t.toMillis(), TimeUnit.MILLISECONDS));

                    Optional.ofNullable(n.getMaxConnectionIdle())
                            .ifPresent(t->builder.maxConnectionIdle(t.toMillis(), TimeUnit.MILLISECONDS));

                    Optional.ofNullable(n.getMaxConcurrentCallsPerConnection())
                            .ifPresent(builder::maxConcurrentCallsPerConnection);

                    Optional.ofNullable(n.getPermitKeepAliveWithoutCalls())
                            .ifPresent(builder::permitKeepAliveWithoutCalls);

                    Optional.ofNullable(n.getMaxInboundMessageSize())
                            .ifPresent(s->builder.maxInboundMessageSize((int)s.toBytes()));

                    Optional.ofNullable(n.getMaxInboundMetadataSize())
                            .ifPresent(s->builder.maxInboundMetadataSize((int)s.toBytes()));


                    return builder;

                })
                .orElse(ServerBuilder.forPort(grpcServerProperties.getRunningPort()));
    }
}
