package org.lognet.springboot.grpc.autoconfigure.consul;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "grpc.consul")
public  class GrpcConsulProperties {
    ServiceRegistrationMode registrationMode = ServiceRegistrationMode.SINGLE_SERVER_WITH_GLOBAL_CHECK;
    @NestedConfigurationProperty
    ConsulDiscoveryProperties discovery;
}
