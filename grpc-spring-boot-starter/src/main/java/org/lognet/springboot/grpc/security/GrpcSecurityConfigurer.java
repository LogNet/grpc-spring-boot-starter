package org.lognet.springboot.grpc.security;

import io.grpc.ServerInterceptor;
import org.springframework.security.config.annotation.SecurityBuilder;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;

public abstract class GrpcSecurityConfigurer <T extends SecurityBuilder<ServerInterceptor>>
        extends SecurityConfigurerAdapter<ServerInterceptor, T> {
}
