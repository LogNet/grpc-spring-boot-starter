package org.lognet.springboot.grpc.autoconfigure.security;

import org.lognet.springboot.grpc.security.BearerTokenAuthSchemeSelector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2Error;

@Configuration
@ConditionalOnClass(OAuth2Error.class)
public class BearerTokenSecurityAutoConfiguration {

    @Bean
    public BearerTokenAuthSchemeSelector bearerTokenAuthSchemeSelector() {
        return new BearerTokenAuthSchemeSelector();
    }
}
