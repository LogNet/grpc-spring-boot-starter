package org.lognet.springboot.grpc.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.BearerTokenError;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.security.oauth2.server.resource.BearerTokenErrors.invalidToken;

public class BearerTokenAuthSchemeSelector implements AuthenticationSchemeSelector{

    private static final Pattern authorizationPattern = Pattern.compile(
            "^Bearer (?<token>[a-zA-Z0-9-._~+/]+)=*$",
            Pattern.CASE_INSENSITIVE);
    private static final String PREFIX ="Bearer";

    @Override
    public Optional<Authentication> getAuthScheme(CharSequence authorization) {

        if(authorization.subSequence(0, PREFIX.length()).toString().equalsIgnoreCase(PREFIX)) {
            Matcher matcher = authorizationPattern.matcher(authorization);

            if (!matcher.matches()) {
                BearerTokenError error = invalidToken("Bearer token is malformed");
                throw new OAuth2AuthenticationException(error);
            }

            String token = matcher.group("token");


            return Optional.of(new BearerTokenAuthenticationToken(token));
        }else {
            return Optional.empty();
        }

    }
}
