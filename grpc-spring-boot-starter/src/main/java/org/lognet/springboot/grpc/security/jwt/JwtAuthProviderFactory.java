package org.lognet.springboot.grpc.security.jwt;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JwtAuthProviderFactory {
    /**
     * Creates  {@link JwtAuthenticationProvider} that emits <b>roles</b> from JWT token clain as {@link GrantedAuthority}
     * @param jwtDecoder JWT token decoder
     * @return JwtAuthenticationProvider
     */
    public static JwtAuthenticationProvider forRoles(JwtDecoder jwtDecoder){
        final JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            final String claim = Optional.ofNullable(jwt.getClaimAsString("aud"))
                    .orElse(jwt.getClaimAsString("azp"));
            Assert.hasText(claim,"Neither 'aud' nor 'azp' claims exist");

            var resource_access = jwt.getClaimAsMap("resource_access");
            Object roles = (( Map<String, Object>) resource_access.get(claim)).get("roles");
            List<String> rolesList = (List<String>) roles;


            return IntStream.range(0, rolesList.size())
                    .mapToObj(k ->  new SimpleGrantedAuthority("ROLE_" + rolesList.get(k)))
                    .collect(Collectors.toList());



        });
        final JwtAuthenticationProvider authenticationProvider = new JwtAuthenticationProvider(jwtDecoder);
        authenticationProvider.setJwtAuthenticationConverter(authenticationConverter);
        return authenticationProvider;
    }

    /**
     * Creates  {@link JwtAuthenticationProvider} that emits <b>authorities</b> from JWT token claim as {@link GrantedAuthority}
     * @param jwtDecoder JWT token decoder
     * @return JwtAuthenticationProvider
     */
    public static JwtAuthenticationProvider forAuthorities(JwtDecoder jwtDecoder){
        return  new  JwtAuthenticationProvider(jwtDecoder);
    }
}
