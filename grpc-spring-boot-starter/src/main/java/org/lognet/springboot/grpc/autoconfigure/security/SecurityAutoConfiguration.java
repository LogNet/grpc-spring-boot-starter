package org.lognet.springboot.grpc.autoconfigure.security;

import org.lognet.springboot.grpc.GRpcServerRunner;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.autoconfigure.GRpcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

@Configuration
@AutoConfigureAfter({GRpcAutoConfiguration.class})
@ConditionalOnBean(value = {GRpcServerRunner.class})
@ConditionalOnProperty(value = "grpc.security.auth.enabled", matchIfMissing = true, havingValue = "true")
@ConditionalOnClass(AuthenticationConfiguration.class)
@Import(GrpcSecurityEnablerConfiguration.class)
public class SecurityAutoConfiguration {
}
