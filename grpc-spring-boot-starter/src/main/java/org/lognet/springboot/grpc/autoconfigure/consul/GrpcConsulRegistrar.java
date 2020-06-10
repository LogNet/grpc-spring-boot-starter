package org.lognet.springboot.grpc.autoconfigure.consul;

import com.ecwid.consul.v1.agent.model.NewService;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.lognet.springboot.grpc.context.GRpcServerInitializedEvent;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;

import java.util.Objects;
import java.util.stream.Collectors;

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


        ConsulDiscoveryProperties consulProperties = applicationContext.getBean(ConsulDiscoveryProperties.class);
        GRpcServerProperties gRpcServerProperties = event.getApplicationContext().getBean(GRpcServerProperties.class);

        NewService grpcService = new NewService();
        grpcService.setPort(event.getServer().getPort());
        if (!consulProperties.isPreferAgentAddress()) {
            grpcService.setAddress(consulProperties.getHostname());
        }
        String appName = "grpc-" + ConsulAutoRegistration.getAppName(consulProperties, applicationContext.getEnvironment());
        grpcService.setName(ConsulAutoRegistration.normalizeForDns(appName));
        grpcService.setId("grpc-" + ConsulAutoRegistration.getInstanceId(consulProperties, applicationContext));
        grpcService.setTags(ConsulAutoRegistration.createTags(consulProperties)
                .stream()
                .filter(t->!t.startsWith("secure="))
                .collect(Collectors.toList())
        );


        if(consulProperties.isRegisterHealthCheck()) {
            GRpcConsulHealthCheck health = GRpcConsulHealthCheck.builder()
                    .socketAddr(consulProperties.getHostname() + ":" + event.getServer().getPort())
                    .grpcUseTlc(Objects.nonNull(gRpcServerProperties.getSecurity()))
                    .interval(consulProperties.getHealthCheckInterval())
                    .timeout(consulProperties.getHealthCheckTimeout())
                    .build();

            health.setDeregisterCriticalServiceAfter(consulProperties.getHealthCheckCriticalTimeout());

            grpcService.setCheck(health);
        }



        return new ConsulRegistration(grpcService, consulProperties);
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
