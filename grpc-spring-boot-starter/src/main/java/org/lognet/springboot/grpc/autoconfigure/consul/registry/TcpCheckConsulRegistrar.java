package org.lognet.springboot.grpc.autoconfigure.consul.registry;

import com.ecwid.consul.v1.agent.model.NewService;
import org.lognet.springboot.grpc.context.GRpcServerInitializedEvent;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistry;

public class TcpCheckConsulRegistrar extends ConsulRegistrar {

    public TcpCheckConsulRegistrar(ConsulServiceRegistry consulServiceRegistry) {
        super(consulServiceRegistry);
    }

    ConsulRegistration getRegistration(GRpcServerInitializedEvent event) {
        ConsulDiscoveryProperties consulProperties = event.getApplicationContext().getBean(ConsulDiscoveryProperties.class);
        NewService service = createNewService(event.getApplicationContext(), event.getServer().getPort(), consulProperties);
        return new ConsulRegistration(service, consulProperties);
    }

}
