package org.lognet.springboot.grpc.autoconfigure.consul;

import com.ecwid.consul.v1.agent.model.NewService;
import org.lognet.springboot.grpc.context.GRpcServerInitializedEvent;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;

public class GrpcConsulRegistrar implements SmartLifecycle {

    private final ConsulServiceRegistry consulServiceRegistry;

    private ConsulRegistration registration;

    public GrpcConsulRegistrar(ConsulServiceRegistry consulServiceRegistry) {
        this.consulServiceRegistry = consulServiceRegistry;
    }

    @EventListener
    public void onGrpcServerStarted(GRpcServerInitializedEvent initializedEvent) {
        registration = getRegistration(initializedEvent);
        consulServiceRegistry.register(registration);
    }

    private ConsulRegistration getRegistration(GRpcServerInitializedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();


        ConsulDiscoveryProperties properties = applicationContext.getBean(ConsulDiscoveryProperties.class);

        NewService grpcService = new NewService();
        grpcService.setPort(event.getServer().getPort());
        if (!properties.isPreferAgentAddress()) {
            grpcService.setAddress(properties.getHostname());
        }
        String appName = "grpc-" + ConsulAutoRegistration.getAppName(properties, applicationContext.getEnvironment());
        grpcService.setName(ConsulAutoRegistration.normalizeForDns(appName));
        grpcService.setId("grpc-" + ConsulAutoRegistration.getInstanceId(properties, applicationContext));

/*
        service.setTags(createTags(properties));
        setCheck(service, autoServiceRegistrationProperties, properties, context,
                    heartbeatProperties);





        */


        return new ConsulRegistration(grpcService, properties);
    }


    @Override
    public boolean isAutoStartup() {
        return false;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public void start() {

    }

    @Override
    public synchronized void stop() {

        consulServiceRegistry.deregister(registration);
        consulServiceRegistry.close();
        registration = null;

    }

    @Override
    public synchronized boolean isRunning() {
        return null != registration;
    }

    @Override
    public int getPhase() {
        return 0;
    }
}
