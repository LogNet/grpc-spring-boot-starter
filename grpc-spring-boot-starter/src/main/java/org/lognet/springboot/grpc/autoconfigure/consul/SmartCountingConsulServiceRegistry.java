package org.lognet.springboot.grpc.autoconfigure.consul;

import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration;

import java.util.concurrent.atomic.AtomicInteger;

public class SmartCountingConsulServiceRegistry implements ServiceRegistry<ConsulRegistration> {

    private ServiceRegistry<ConsulRegistration> target;
    private AtomicInteger count = new AtomicInteger(0);

    public SmartCountingConsulServiceRegistry(ServiceRegistry<ConsulRegistration> target) {
        this.target = target;
    }

    @Override
    public void register(ConsulRegistration registration) {
        target.register(registration);
        count.incrementAndGet();
    }

    @Override
    public void deregister(ConsulRegistration registration) {
        target.deregister(registration);
        count.decrementAndGet();
    }

    @Override
    public void close() {
        if(0==count.intValue()){
            target.close();
        }
    }

    @Override
    public void setStatus(ConsulRegistration registration, String status) {
        target.setStatus(registration,status);
    }

    @Override
    public <T> T getStatus(ConsulRegistration registration) {
        return target.getStatus(registration);
    }
}
