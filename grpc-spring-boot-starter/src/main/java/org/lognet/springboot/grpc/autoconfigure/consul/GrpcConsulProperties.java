package org.lognet.springboot.grpc.autoconfigure.consul;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("grpc.discovery.consul")
@Getter
@Setter
public class GrpcConsulProperties {

  /** Service name, default is "grpc-application-name" */
  private String serviceName;

  /** Tags to use when registering service. */
  private List<String> tags;
}
