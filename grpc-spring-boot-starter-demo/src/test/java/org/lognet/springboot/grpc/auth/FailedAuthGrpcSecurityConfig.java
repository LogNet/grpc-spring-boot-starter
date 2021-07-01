package org.lognet.springboot.grpc.auth;

import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

public class FailedAuthGrpcSecurityConfig extends GrpcSecurityConfigurerAdapter {
    @Override
    public void configure(GrpcSecurity builder) throws Exception {

        builder.authorizeRequests()
                .anyMethod().authenticated()
                .and()
                .authenticationProvider(new AuthenticationProvider() {
                    @Override
                    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                        throw  new BadCredentialsException("");
                    }

                    @Override
                    public boolean supports(Class<?> authentication) {
                        return true;
                    }
                })
                .userDetailsService(new InMemoryUserDetailsManager());
    }
}
