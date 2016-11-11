package org.lognet.springboot.grpc.register;

/**
 * Clone from https://github.com/rucky2013/grpc-spring-boot-consul.git
 */
public interface RpcRegister {
    void registerRpc(ServiceInfo serviceInfo);

    void unRegisterRpc(String serviceId);
}
