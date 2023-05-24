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
    public Optional<Authentication> getAuthScheme(CharSequence authorization) {
        final List<Authentication> auth = selectors
                .stream()
                .map(selector -> selector.getAuthScheme(authorization))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        switch (auth.size()){
            case 0:
                log.error(String.format("Authentication scheme '%s' is not supported.",
                        Optional.ofNullable(authorization)
                                .map(s->s.toString().split(" ",2)[0])
                                .orElse(null)
                ));
                return Optional.empty();
            case 1 :
                return Optional.of(auth.get(0));
            default:
                throw  new IllegalStateException("Ambiguous authentication scheme "+authorization.toString());
        }
    }

    @Override
    public AuthenticationSchemeRegistry register(AuthenticationSchemeSelector selector) {
        selectors.add(selector);
        return this;
    }
}
