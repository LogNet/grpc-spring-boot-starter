package org.lognet.springboot.grpc.autoconfigure.consul.registry;

import com.ecwid.consul.v1.agent.model.NewService;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.lognet.springboot.grpc.autoconfigure.consul.GRpcConsulHealthCheck;
import org.lognet.springboot.grpc.context.GRpcServerInitializedEvent;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistry;
import org.springframework.context.ApplicationContext;

import java.util.Objects;

/**
 * created by Yehor Kravchenko
 * 05/02/2020 - 12:31
 */

public class GrpcCheckConsulRegistrar extends ConsulRegistrar {

    public GrpcCheckConsulRegistrar(ConsulServiceRegistry consulServiceRegistry) {
        super(consulServiceRegistry);
    }

    @Override
    ConsulRegistration getRegistration(GRpcServerInitializedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();
        ConsulDiscoveryProperties consulProperties = applicationContext.getBean(ConsulDiscoveryProperties.class);
        GRpcServerProperties gRpcServerProperties = event.getApplicationContext().getBean(GRpcServerProperties.class);

        NewService service = createNewService(event.getApplicationContext(), event.getServer().getPort(), consulProperties);

        GRpcConsulHealthCheck health = GRpcConsulHealthCheck.builder()
                .socketAddr(consulProperties.getHostname() + ":" + event.getServer().getPort())
                .grpcUseTlc(Objects.nonNull(gRpcServerProperties.getSecurity()))
                .interval(consulProperties.getHealthCheckInterval())
                .timeout(consulProperties.getHealthCheckTimeout())
                .build();

        health.setDeregisterCriticalServiceAfter(consulProperties.getHealthCheckCriticalTimeout());

        service.setCheck(health);
        return new ConsulRegistration(service, consulProperties);
    }

}
