package org.lognet.springboot.grpc.health;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.stub.StreamObserver;

public class DefaultHealthStatusService extends ManagedHealthStatusService {
    private final HealthStatusManager healthStatusManager = new HealthStatusManager();
    private final HealthGrpc.HealthImplBase service = (HealthGrpc.HealthImplBase) healthStatusManager.getHealthService();

    @Override
    public void  onShutdown() {
         healthStatusManager.enterTerminalState();
    }

    @Override
    public void setStatus(String service, HealthCheckResponse.ServingStatus status) {
        healthStatusManager.setStatus(service,status);
    }

    @Override
    public void check(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        service.check(request, responseObserver);
    }

    @Override
    public void watch(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        service.watch(request, responseObserver);
    }
}
