package org.lognet.springboot.grpc.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
class AuthenticationSchemeService implements AuthenticationSchemeRegistry, AuthenticationSchemeSelector{


    private  List<AuthenticationSchemeSelector> selectors = new ArrayList<>();
    @Override
    public Optional<Authentication> getAuthScheme(String authorization) {
        final List<Authentication> auth = selectors
                .stream()
                .map(selector -> selector.getAuthScheme(authorization))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        switch (auth.size()){
            case 0:
                throw  new IllegalStateException("Authentication scheme not supported");
            case 1 :
                return Optional.of(auth.get(0));
            default:
                throw  new IllegalStateException("Ambiguous authentication scheme");
        }
    }

    @Override
    public AuthenticationSchemeRegistry register(AuthenticationSchemeSelector selector) {
        selectors.add(selector);
        return this;
    }
}
