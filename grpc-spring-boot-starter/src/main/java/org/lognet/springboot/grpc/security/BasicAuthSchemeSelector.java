package org.lognet.springboot.grpc.security;


import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Base64;
import java.util.Optional;

public class BasicAuthSchemeSelector implements AuthenticationSchemeSelector{


    private final static String prefix="Basic ";


    @Override
    public Optional<Authentication> getAuthScheme(CharSequence authorization) {


            if (authorization.subSequence(0,prefix.length()).toString().equalsIgnoreCase(prefix)) {

                String token = new String(Base64.getDecoder().decode(authorization.subSequence(prefix.length(),authorization.length()).toString()));
                int delim = token.indexOf(":");
                if (delim == -1) {
                    throw new BadCredentialsException("Invalid basic authentication token");
                }
                return Optional.of(new UsernamePasswordAuthenticationToken(token.substring(0, delim), token.substring(delim + 1)));
            }
            return Optional.empty();






        }

}
