package org.lognet.springboot.grpc.health;

import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;

public interface GRpcHealthStatusManager {

    /**
     * Invoked on server shutdown. Implementation is advised to set status of all services as ServingStatus.NOT_SERVING
     */
    void onShutdown();

    /**
     * Invoked on startup with {@link io.grpc.health.v1.HealthCheckResponse.ServingStatus#SERVING } for each discovered grpc service name
     * @param service - grpc service name
     * @param status - new status
     */
    void setStatus(String service, HealthCheckResponse.ServingStatus status);


    HealthGrpc.HealthImplBase getHealthService() ;

}
