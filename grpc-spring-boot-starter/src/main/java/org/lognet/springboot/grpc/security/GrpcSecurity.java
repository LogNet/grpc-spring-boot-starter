package org.lognet.springboot.grpc.security;

import io.grpc.Context;
import io.grpc.ServerInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.lognet.springboot.grpc.GRpcServicesRegistry;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.ExpressionBasedAnnotationAttributeFactory;
import org.springframework.security.access.expression.method.ExpressionBasedPostInvocationAdvice;
import org.springframework.security.access.expression.method.ExpressionBasedPreInvocationAdvice;
import org.springframework.security.access.intercept.AfterInvocationManager;
import org.springframework.security.access.intercept.AfterInvocationProviderManager;
import org.springframework.security.access.method.DelegatingMethodSecurityMetadataSource;
import org.springframework.security.access.prepost.PostInvocationAdviceProvider;
import org.springframework.security.access.prepost.PreInvocationAuthorizationAdviceVoter;
import org.springframework.security.access.prepost.PrePostAnnotationSecurityMetadataSource;
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
import java.util.Collection;
import java.util.Optional;

public class GrpcSecurity extends AbstractConfiguredSecurityBuilder<ServerInterceptor, GrpcSecurity>
        implements SecurityBuilder<ServerInterceptor>, ApplicationContextAware {

    private  ApplicationContext applicationContext;

    public static final Context.Key<Authentication> AUTHENTICATION_CONTEXT_KEY =  Context.key("AUTHENTICATION");
    public GrpcSecurity(ObjectPostProcessor<Object> objectPostProcessor) {
        super(objectPostProcessor);

    }

    public GrpcServiceAuthorizationConfigurer.Registry authorizeRequests()
            throws Exception {

        return getOrApply(new GrpcServiceAuthorizationConfigurer (applicationContext.getBean(GRpcServicesRegistry.class)))
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



        final GrpcSecurityMetadataSource metadataSource =getSharedObject(GrpcSecurityMetadataSource.class);
        DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler = new DefaultMethodSecurityExpressionHandler();
        methodSecurityExpressionHandler.setApplicationContext(getApplicationContext());
        final DelegatingMethodSecurityMetadataSource compositeMDS = new DelegatingMethodSecurityMetadataSource(Arrays.asList(
                metadataSource,
                new PrePostAnnotationSecurityMetadataSource(
                        new ExpressionBasedAnnotationAttributeFactory(
                                methodSecurityExpressionHandler
                        )
                )
        ));
        final SecurityInterceptor securityInterceptor = new SecurityInterceptor(compositeMDS,getAuthenticationSchemeService());
        securityInterceptor.setAfterInvocationManager(afterInvocationManager());
        securityInterceptor.setAuthenticationManager(getSharedObject(AuthenticationManagerBuilder.class).build());
        final RoleVoter scopeVoter = new RoleVoter();
        scopeVoter.setRolePrefix("SCOPE_");



        ExpressionBasedPreInvocationAdvice expressionAdvice = new ExpressionBasedPreInvocationAdvice();
        expressionAdvice.setExpressionHandler(methodSecurityExpressionHandler);


        final AffirmativeBased accessDecisionManager = new AffirmativeBased(Arrays.asList(
                new RoleVoter(),
                scopeVoter,
                new AuthenticatedAttributeVoter(),
                new PreInvocationAuthorizationAdviceVoter(expressionAdvice){
                    @Override
                    public int vote(Authentication authentication, MethodInvocation method, Collection<ConfigAttribute> attributes) {
                        // first time invoked without arguments
                        return null==method.getArguments() ? ACCESS_ABSTAIN: super.vote(authentication, method, attributes);
                    }
                }
        ));
        accessDecisionManager.setAllowIfAllAbstainDecisions(true);

        securityInterceptor.setAccessDecisionManager(accessDecisionManager);
        final GRpcServerProperties.SecurityProperties.Auth authCfg = Optional.of(applicationContext.getBean(GRpcServerProperties.class))
                .map(GRpcServerProperties::getSecurity)
                .map(GRpcServerProperties.SecurityProperties::getAuth)
                .orElse(null);
        securityInterceptor.setConfig(authCfg);
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

    protected AfterInvocationManager afterInvocationManager() {

            AfterInvocationProviderManager invocationProviderManager = new AfterInvocationProviderManager();
            ExpressionBasedPostInvocationAdvice postAdvice = new ExpressionBasedPostInvocationAdvice(
                    new DefaultMethodSecurityExpressionHandler());
            PostInvocationAdviceProvider postInvocationAdviceProvider = new PostInvocationAdviceProvider(postAdvice){
                @Override
                public boolean supports(Class<?> clazz) {
                    return MethodInvocation.class.isAssignableFrom(clazz); //todo : remove once fixed https://github.com/spring-projects/spring-security/issues/10236
                }
            };



            invocationProviderManager.setProviders(Arrays.asList(
                    postInvocationAdviceProvider
            ));
            invocationProviderManager.afterPropertiesSet();
            return invocationProviderManager;

    }


}
