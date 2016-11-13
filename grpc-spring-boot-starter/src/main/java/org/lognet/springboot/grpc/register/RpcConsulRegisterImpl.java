package org.lognet.springboot.grpc.register;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Clone from https://github.com/rucky2013/grpc-spring-boot-consul.git
 * from RpcRegisterImpl to RpcConsulRegisterImpl
 */
@Component
@Slf4j
public class RpcConsulRegisterImpl implements RpcRegister {

    @Autowired
    private GRpcServerProperties gRpcServerProperties;

    @Override
    public void registerRpc(ServiceInfo serviceInfo) {
        log.debug("RpcConsulRegisterImpl.registerRpc ... Registring ServiceInfo {}", serviceInfo);
        Consul consul = Consul.builder().withHostAndPort(HostAndPort.fromString(gRpcServerProperties.getConsulHost())).build();
        AgentClient agentClient = consul.agentClient();
        String serviceId = gRpcServerProperties.getLocalIp() + ":" + gRpcServerProperties.getPort();
        String healthUrl = gRpcServerProperties.getHealthUrl();   //String.format("http://%s:8080/health", gRpcServerProperties.getLocalIp());
        Registration registration = ImmutableRegistration.builder().address(gRpcServerProperties.getLocalIp()).port(gRpcServerProperties.getPort())
                .id(serviceId).name(serviceInfo.getName()).addTags(serviceInfo.getVersion())
                .check(Registration.RegCheck.http(healthUrl, 5)).build();
        agentClient.register(registration);
        log.info("grpc service register to consul:{},health url:{}", serviceInfo, healthUrl);
    }

    @Override
    public void unRegisterRpc(String serviceId) {

    }
}
