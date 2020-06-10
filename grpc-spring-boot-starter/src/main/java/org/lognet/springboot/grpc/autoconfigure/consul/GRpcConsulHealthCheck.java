package org.lognet.springboot.grpc.autoconfigure.consul;

import com.ecwid.consul.v1.agent.model.NewService;
import com.google.gson.annotations.SerializedName;
import lombok.*;

/**
 * created by Yehor Kravchenko
 * 04/02/2020 - 16:20
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder
public class GRpcConsulHealthCheck extends NewService.Check {

    /**
     * Host address + port
     */
    @SerializedName("grpc")
    private String socketAddr;

    /**
     * interval for health check polling
     */
    @Builder.Default
    private String interval = "30s";

    /**
     * Is server used TLS
     */
    @SerializedName("grpc_use_tls")
    private boolean grpcUseTlc;

    /**
     * timeout on healthcheck polling
     */
    private String timeout;
}