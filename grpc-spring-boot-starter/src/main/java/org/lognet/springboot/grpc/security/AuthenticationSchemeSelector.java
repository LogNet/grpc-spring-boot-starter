package org.lognet.springboot.grpc.security;

import org.springframework.security.core.Authentication;

import java.util.Optional;

public interface AuthenticationSchemeSelector {
    Optional<Authentication> getAuthScheme(String authorization);
}
