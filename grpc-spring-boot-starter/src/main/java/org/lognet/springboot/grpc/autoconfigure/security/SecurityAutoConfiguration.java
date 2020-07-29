package org.lognet.springboot.grpc.autoconfigure.security;

import org.lognet.springboot.grpc.security.EnableGrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(GrpcSecurityConfigurerAdapter.class)
@EnableGrpcSecurity
public class SecurityAutoConfiguration {
}
