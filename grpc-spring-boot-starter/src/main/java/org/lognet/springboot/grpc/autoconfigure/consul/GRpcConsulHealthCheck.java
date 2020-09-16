package org.lognet.springboot.grpc.autoconfigure.consul;

import com.ecwid.consul.v1.agent.model.NewService;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * created by Yehor Kravchenko
 * 04/02/2020 - 16:20
 */
@Getter
@ToString
@Builder
public class GRpcConsulHealthCheck extends NewService.Check {

    /**
     * Host address + port
     */
    @SerializedName("grpc")
    private final String socketAddr;

    /**
     * interval for health check polling
     */
    @Builder.Default
    private final String interval = "30s";

    /**
     * Is server used TLS
     */
    @SerializedName("grpc_use_tls")
    private final boolean grpcUseTlc;

    /**
     * timeout on healthcheck polling
     */
    private final String timeout;
}