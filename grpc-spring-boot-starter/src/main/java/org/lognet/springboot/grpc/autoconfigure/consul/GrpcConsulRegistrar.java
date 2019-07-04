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

    private ConsulRegistration registration;
    private ConsulServiceRegistry consulServiceRegistry;

    public GrpcConsulRegistrar(ConsulServiceRegistry consulServiceRegistry) {
        this.consulServiceRegistry = consulServiceRegistry;
    }

    @EventListener
    public void onGrpcServerStarted(GRpcServerInitializedEvent initializedEvent) {
        registration = getRegistration(initializedEvent);
        consulServiceRegistry.register(registration);
    }

    private ConsulRegistration getRegistration(GRpcServerInitializedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        ConsulDiscoveryProperties properties = context.getBean(ConsulDiscoveryProperties.class);
        GrpcConsulProperties discoveryProperties = context.getBean(GrpcConsulProperties.class);

        NewService service = new NewService();
        service.setPort(event.getServer().getPort());

        /** Set address */
        if (!properties.isPreferAgentAddress()) {
            service.setAddress(properties.getHostname());
        }

        /** Set service name */
        String serviceName = discoveryProperties.getServiceName() != null
            && !discoveryProperties.getServiceName().isEmpty()
            ? discoveryProperties.getServiceName()
            : "grpc" + ConsulAutoRegistration.SEPARATOR + ConsulAutoRegistration.getAppName(
                properties, context.getEnvironment()
            );
        service.setName(ConsulAutoRegistration.normalizeForDns(serviceName));

        /** Set service id */
        service.setId("grpc" + ConsulAutoRegistration.SEPARATOR +
            ConsulAutoRegistration.getInstanceId(properties, context)
        );

        /** Set service tags */
        if (discoveryProperties.getTags() != null) {
            service.setTags(discoveryProperties.getTags());
        }

        return new ConsulRegistration(service, properties);
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
