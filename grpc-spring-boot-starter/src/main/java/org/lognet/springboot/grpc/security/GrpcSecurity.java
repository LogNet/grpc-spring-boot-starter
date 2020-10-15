package org.lognet.springboot.grpc.security;

import io.grpc.Context;
import io.grpc.ServerInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.AbstractConfiguredSecurityBuilder;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.SecurityBuilder;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Arrays;

public class GrpcSecurity extends AbstractConfiguredSecurityBuilder<ServerInterceptor, GrpcSecurity>
        implements SecurityBuilder<ServerInterceptor>, ApplicationContextAware {

    private  ApplicationContext applicationContext;

    public static final Context.Key<Authentication> AUTHENTICATION_CONTEXT_KEY =  Context.key("AUTHENTICATION");
    public GrpcSecurity(ObjectPostProcessor<Object> objectPostProcessor) {
        super(objectPostProcessor);

    }

    public GrpcServiceAuthorizationConfigurer.Registry authorizeRequests()
            throws Exception {

        return getOrApply(new GrpcServiceAuthorizationConfigurer (applicationContext))
                .getRegistry();
    }

    public GrpcSecurity userDetailsService(UserDetailsService userDetailsService)
            throws Exception {
        getAuthenticationRegistry().userDetailsService(userDetailsService);
        return this;
    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public GrpcSecurity authenticationSchemeSelector(AuthenticationSchemeSelector selector) {
        getAuthenticationSchemeService().register(selector);
        return this;
    }

    public GrpcSecurity authenticationProvider(AuthenticationProvider authenticationProvider) {
        getAuthenticationRegistry().authenticationProvider(authenticationProvider);
        return this;
    }

    @Override
    protected void beforeConfigure() throws Exception {

    }

    @Override
    protected ServerInterceptor performBuild() throws Exception {

        final SecurityInterceptor securityInterceptor = new SecurityInterceptor(getSharedObject(GrpcSecurityMetadataSource.class),
                getAuthenticationSchemeService());
        securityInterceptor.setAuthenticationManager(getSharedObject(AuthenticationManagerBuilder.class).build());
        final RoleVoter scopeVoter = new RoleVoter();
        scopeVoter.setRolePrefix("SCOPE_");
        securityInterceptor.setAccessDecisionManager(new AffirmativeBased(Arrays.asList(new RoleVoter(),scopeVoter, new AuthenticatedAttributeVoter())));
        return securityInterceptor;
    }
    @SuppressWarnings("unchecked")
    private <C extends SecurityConfigurerAdapter<ServerInterceptor, GrpcSecurity>> C getOrApply(C configurer) throws Exception {
        C existingConfig = (C) getConfigurer(configurer.getClass());
        if (existingConfig != null) {
            return existingConfig;
        }
        return apply(configurer);
    }
    private AuthenticationManagerBuilder getAuthenticationRegistry() {
        return getSharedObject(AuthenticationManagerBuilder.class);
    }
    private AuthenticationSchemeService getAuthenticationSchemeService() {
        return getSharedObject(AuthenticationSchemeService.class);
    }
}
