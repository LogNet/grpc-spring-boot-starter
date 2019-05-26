package org.lognet.springboot.grpc.autoconfigure.consul;

import com.ecwid.consul.v1.agent.model.NewService;
import org.lognet.springboot.grpc.context.GRpcServerInitializedEvent;
import org.lognet.springboot.grpc.context.GRpcServerStoppedEvent;
import org.lognet.springboot.grpc.context.GrpcServerEvent;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;

public class GrpcConsulRegistrar {

    private ConsulRegistration registration;
    private ServiceRegistry<ConsulRegistration> consulServiceRegistry;

    public GrpcConsulRegistrar(ServiceRegistry<ConsulRegistration>  consulServiceRegistry) {
        this.consulServiceRegistry = consulServiceRegistry;
    }

    @EventListener
    public void onGrpcServerStarted(GRpcServerInitializedEvent initializedEvent) {
        registration = getRegistration(initializedEvent);
        consulServiceRegistry.register(registration);
    }
    @EventListener
    public void onGrpcServerStopped(GRpcServerStoppedEvent initializedEvent) {

        consulServiceRegistry.deregister(registration);
        consulServiceRegistry.close();
    }



    private ConsulRegistration getRegistration(GrpcServerEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();


        ConsulDiscoveryProperties properties = applicationContext.getBean(ConsulDiscoveryProperties.class);

        NewService grpcService = new NewService();
        grpcService.setPort(event.getServer().getPort());
        if (!properties.isPreferAgentAddress()) {
            grpcService.setAddress(properties.getHostname());
        }
        String appName = "grpc_" + ConsulAutoRegistration.getAppName(properties, applicationContext.getEnvironment());
        grpcService.setName(ConsulAutoRegistration.normalizeForDns(appName));
        grpcService.setId("grpc_" +ConsulAutoRegistration.getInstanceId(properties, applicationContext));

/*
        service.setTags(createTags(properties));
        setCheck(service, autoServiceRegistrationProperties, properties, context,
                    heartbeatProperties);





        */


        return new ConsulRegistration(grpcService, properties);
    }


}
