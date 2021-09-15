package org.lognet.springboot.grpc.health;

import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.protobuf.services.HealthStatusManager;

public class DefaultHealthStatusManager implements GRpcHealthStatusManager {
    private final HealthStatusManager healthStatusManager = new HealthStatusManager();

    @Override
    public void  onShutdown() {
         healthStatusManager.enterTerminalState();
    }

    @Override
    public void setStatus(String service, HealthCheckResponse.ServingStatus status) {
        healthStatusManager.setStatus(service,status);
    }

    @Override
    public HealthGrpc.HealthImplBase getHealthService() {
         return (HealthGrpc.HealthImplBase) healthStatusManager.getHealthService();
    }
}
