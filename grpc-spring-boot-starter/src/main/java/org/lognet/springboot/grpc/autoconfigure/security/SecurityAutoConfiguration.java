package org.lognet.springboot.grpc.autoconfigure.security;

import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnBean(GrpcSecurityConfigurerAdapter.class)
@Import(GrpcSecurityEnablerConfiguration.class)
public class SecurityAutoConfiguration {
}
