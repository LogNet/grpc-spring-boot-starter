package org.lognet.springboot.grpc.security;

public interface AuthenticationSchemeRegistry {
    AuthenticationSchemeRegistry register(AuthenticationSchemeSelector selector);
}
