package org.lognet.springboot.grpc.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("grpc.validation")
@Getter
@Setter
public class GRpcValidationProperties {
    private Integer interceptorOrder ;
}
