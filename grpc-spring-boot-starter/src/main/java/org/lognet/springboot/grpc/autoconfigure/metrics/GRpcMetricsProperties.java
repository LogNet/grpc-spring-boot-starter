package org.lognet.springboot.grpc.autoconfigure.metrics;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("grpc.metrics")
@Getter
@Setter
public class GRpcMetricsProperties {
    private Integer interceptorOrder ;
}
