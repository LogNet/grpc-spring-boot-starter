package org.lognet.springboot.grpc.autoconfigure.consul;

import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.lognet.springboot.grpc.context.GRpcServerInitializedEvent;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.stream.Collectors;

public class GrpcConsulRegistrar implements SmartLifecycle {

    private final ConsulServiceRegistry consulServiceRegistry;

    private List<ConsulRegistration> registrations;

    public GrpcConsulRegistrar(ConsulServiceRegistry consulServiceRegistry) {
        this.consulServiceRegistry = consulServiceRegistry;
    }

    @EventListener
    public void onGrpcServerStarted(GRpcServerInitializedEvent event) {

        ApplicationContext applicationContext = event.getApplicationContext();
        ConsulDiscoveryProperties consulProperties = applicationContext.getBean(ConsulDiscoveryProperties.class);
        final ServiceRegistrationStrategy registrationStrategy = applicationContext.getBean(GRpcServerProperties.class).getConsul().getRegistrationMode();

        registrations = registrationStrategy.createServices(event.getServer(),applicationContext)
                .stream()
                .map(s->new ConsulRegistration(s, consulProperties))
                .collect(Collectors.toList());

        registrations.forEach(consulServiceRegistry::register);
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

        registrations.forEach(consulServiceRegistry::deregister);
        consulServiceRegistry.close();
        registrations = null;

    }

    @Override
    public synchronized boolean isRunning() {
        return null != registrations;
    }

    @Override
    public int getPhase() {
        return 0;
    }
}
