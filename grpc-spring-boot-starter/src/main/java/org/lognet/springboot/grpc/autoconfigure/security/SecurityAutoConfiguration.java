package org.lognet.springboot.grpc.autoconfigure.security;

import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

@Configuration
@ConditionalOnProperty(value = "grpc.security.auth.enabled", matchIfMissing = true,havingValue = "true")
@ConditionalOnClass(AuthenticationConfiguration.class)
@Import(GrpcSecurityEnablerConfiguration.class)
public class SecurityAutoConfiguration {
}
