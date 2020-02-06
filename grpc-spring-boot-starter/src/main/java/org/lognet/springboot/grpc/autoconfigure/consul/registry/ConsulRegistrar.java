package org.lognet.springboot.grpc.autoconfigure.consul.registry;

import com.ecwid.consul.v1.agent.model.NewService;
import org.lognet.springboot.grpc.context.GRpcServerInitializedEvent;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;

import static org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration.createTags;

/**
 * created by Yehor Kravchenko
 * 05/02/2020 - 12:31
 */

public abstract class ConsulRegistrar implements SmartLifecycle {
    private ConsulServiceRegistry consulServiceRegistry;
    private ConsulRegistration registration;
    private static final String GRPC_SUFFIX = ConsulAutoRegistration.SEPARATOR + "grpc";

    ConsulRegistrar(ConsulServiceRegistry consulServiceRegistry) {
        this.consulServiceRegistry = consulServiceRegistry;
    }

    @EventListener
    public void onGrpcServerStarted(GRpcServerInitializedEvent initializedEvent) {
        registration = getRegistration(initializedEvent);
        consulServiceRegistry.register(registration);
    }

    abstract ConsulRegistration getRegistration(GRpcServerInitializedEvent event);

    NewService createNewService(ApplicationContext ctx, Integer port, ConsulDiscoveryProperties consulProperties) {
        NewService service = new NewService();
        service.setPort(port);

        if (!consulProperties.isPreferAgentAddress()) {
            service.setAddress(consulProperties.getHostname());
        }

        String appName = ConsulAutoRegistration.getAppName(consulProperties, ctx.getEnvironment())
                + ConsulAutoRegistration.SEPARATOR + GRPC_SUFFIX;
        service.setName(ConsulAutoRegistration.normalizeForDns(appName));
        service.setId(ConsulAutoRegistration.getInstanceId(consulProperties, ctx) + GRPC_SUFFIX);
        service.setTags(createTags(consulProperties));
        return service;
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
