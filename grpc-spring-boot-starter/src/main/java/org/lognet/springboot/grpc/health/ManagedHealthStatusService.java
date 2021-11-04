package org.lognet.springboot.grpc.health;

import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;

public abstract class ManagedHealthStatusService extends HealthGrpc.HealthImplBase{

    /**
     * Invoked on server shutdown. Implementation is advised to set status of all services as ServingStatus.NOT_SERVING
     */
    public abstract void onShutdown();

    /**
     * Invoked on startup with {@link io.grpc.health.v1.HealthCheckResponse.ServingStatus#SERVING } for each discovered grpc service name
     * @param service - grpc service name
     * @param status - new status
     */
    public abstract  void setStatus(String service, HealthCheckResponse.ServingStatus status);



}
