package org.lognet.springboot.grpc.health;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@GRpcService
@Configuration
@ConditionalOnMissingBean(ManagedHealthStatusService.class)
public class DefaultHealthStatusService extends ManagedHealthStatusService {
    private final HealthStatusManager healthStatusManager = new HealthStatusManager();
    private final HealthGrpc.HealthImplBase service = (HealthGrpc.HealthImplBase) healthStatusManager.getHealthService();
    private final Map<String, HealthCheckResponse.ServingStatus> statusMap = new ConcurrentHashMap<>();
    private final Map<String, HealthCheckResponse.ServingStatus> unmodifiableStatusMap = Collections.unmodifiableMap(statusMap);
    @Override
    public void  onShutdown() {
         healthStatusManager.enterTerminalState();
    }

    @Override
    public void setStatus(String service, HealthCheckResponse.ServingStatus status) {
        statusMap.put(service,status);
        healthStatusManager.setStatus(service,status);
    }

    @Override
    public Map<String, HealthCheckResponse.ServingStatus> statuses() {
        return unmodifiableStatusMap;
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
