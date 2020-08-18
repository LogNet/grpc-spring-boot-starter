package org.lognet.springboot.grpc.autoconfigure.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(GrpcSecurityEnablerConfiguration.class)
public class SecurityAutoConfiguration {
}
