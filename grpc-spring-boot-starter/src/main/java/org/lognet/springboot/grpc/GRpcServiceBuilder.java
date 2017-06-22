package org.lognet.springboot.grpc;

import io.grpc.ServerInterceptor;

import java.util.Collection;

/**
 * Interface that enables grpc services to dynamically register their own interceptors
 */
public interface GRpcServiceBuilder {
    Collection<ServerInterceptor> getServiceInterceptors();
}
