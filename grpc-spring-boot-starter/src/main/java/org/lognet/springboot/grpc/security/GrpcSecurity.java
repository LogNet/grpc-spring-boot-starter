package org.lognet.springboot.grpc.security;

import io.grpc.Context;
import io.grpc.ServerInterceptor;
import org.lognet.springboot.grpc.GRpcServicesRegistry;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authorization.method.PostAuthorizeAuthorizationManager;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.AbstractConfiguredSecurityBuilder;
import org.springframework.security.config.annotation.SecurityBuilder;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

public class GrpcSecurity extends AbstractConfiguredSecurityBuilder<ServerInterceptor, GrpcSecurity>
        implements SecurityBuilder<ServerInterceptor>, ApplicationContextAware {

    private ApplicationContext applicationContext;

    public static final Context.Key<Authentication> AUTHENTICATION_CONTEXT_KEY = Context.key("AUTHENTICATION");

    public GrpcSecurity(ObjectPostProcessor<Object> objectPostProcessor) {
        super(objectPostProcessor);
    }

    public GrpcServiceAuthorizationConfigurer.Registry authorizeRequests() {
        GrpcServiceAuthorizationConfigurer configurer = getConfigurer(GrpcServiceAuthorizationConfigurer.class);
        if (configurer == null) {
            configurer = new GrpcServiceAuthorizationConfigurer(applicationContext.getBean(GRpcServicesRegistry.class));
            with(configurer);
        }
        return configurer.getRegistry();
    }

    public GrpcSecurity userDetailsService(UserDetailsService userDetailsService) {
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
    protected void beforeConfigure() {
    }

    @Override
    protected ServerInterceptor performBuild() {

        final GrpcSecurityMetadataSource metadataSource = getSharedObject(GrpcSecurityMetadataSource.class);

        // Build expression handler with application context so that Spring beans
        // referenced in @PreAuthorize/@PostAuthorize expressions (e.g. @permissionService) are resolved.
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setApplicationContext(getApplicationContext());

        // PreAuthorize manager — handles @PreAuthorize annotations
        PreAuthorizeAuthorizationManager preAuthorizeManager = new PreAuthorizeAuthorizationManager();
        preAuthorizeManager.setExpressionHandler(expressionHandler);

        // PostAuthorize manager — handles @PostAuthorize annotations
        PostAuthorizeAuthorizationManager postAuthorizeManager = new PostAuthorizeAuthorizationManager();
        postAuthorizeManager.setExpressionHandler(expressionHandler);

        // Note: @Secured annotations are handled by GrpcServiceAuthorizationConfigurer.processSecuredAnnotation()
        // at startup time and added to the GrpcSecurityMetadataSource. They are NOT handled by a
        // SecuredAuthorizationManager here, to preserve AffirmativeBased semantics (if any manager
        // grants, access is granted regardless of @Secured denial).

        final SecurityInterceptor securityInterceptor = new SecurityInterceptor(
                metadataSource,
                getAuthenticationSchemeService(),
                preAuthorizeManager,
                postAuthorizeManager
        );

        securityInterceptor.setAuthenticationManager(
                getSharedObject(AuthenticationManagerBuilder.class).build()
        );

        final GRpcServerProperties.SecurityProperties.Auth authCfg = Optional.of(applicationContext.getBean(GRpcServerProperties.class))
                .map(GRpcServerProperties::getSecurity)
                .map(GRpcServerProperties.SecurityProperties::getAuth)
                .orElse(null);
        securityInterceptor.setConfig(authCfg);
        return securityInterceptor;
    }

    private AuthenticationManagerBuilder getAuthenticationRegistry() {
        return getSharedObject(AuthenticationManagerBuilder.class);
    }

    private AuthenticationSchemeService getAuthenticationSchemeService() {
        return getSharedObject(AuthenticationSchemeService.class);
    }
}
