package org.lognet.springboot.grpc.security.jwt;

import com.nimbusds.jose.shaded.json.JSONNavi;
import com.nimbusds.jose.shaded.json.JSONObject;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.util.Assert;

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

            JSONObject resourceAccess = jwt.getClaim("resource_access");

            final JSONNavi<?> roles = JSONNavi.newInstanceArray()
                    .add(resourceAccess)
                    .at(0)
                    .at(claim)
                    .at("roles");

            return IntStream.range(0, roles.getSize())
                    .mapToObj(k ->  new SimpleGrantedAuthority("ROLE_" + roles.get(k).toString()))
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
