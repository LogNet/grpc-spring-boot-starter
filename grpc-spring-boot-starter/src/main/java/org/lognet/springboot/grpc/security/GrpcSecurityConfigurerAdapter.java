package org.lognet.springboot.grpc.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

public abstract class GrpcSecurityConfigurerAdapter extends GrpcSecurityConfigurer<GrpcSecurity> {

    private AuthenticationConfiguration authenticationConfiguration;
    private AuthenticationManagerBuilder authenticationManagerBuilder;

    protected GrpcSecurityConfigurerAdapter() {
    }


    @Autowired
    public void setApplicationContext(ApplicationContext context) throws Exception {


        ObjectPostProcessor<Object> objectPostProcessor = context.getBean(ObjectPostProcessor.class);
        this.authenticationConfiguration = context.getBean(AuthenticationConfiguration.class);

        authenticationManagerBuilder = authenticationConfiguration.authenticationManagerBuilder(objectPostProcessor,context)
        .parentAuthenticationManager(authenticationConfiguration.getAuthenticationManager());


    }

    @Override
    public void init(GrpcSecurity builder) throws Exception {
        builder.apply(new GrpcServiceAuthorizationConfigurer(builder.getApplicationContext()));
        builder.setSharedObject(AuthenticationManagerBuilder.class,authenticationManagerBuilder);
        final AuthenticationSchemeService authenticationSchemeService = new AuthenticationSchemeService();
        authenticationSchemeService.register(new BasicAuthSchemeSelector());
        authenticationSchemeService.register(new BearerTokenAuthSchemeSelector());
        builder.setSharedObject(AuthenticationSchemeService.class, authenticationSchemeService);

    }

    @Override
    public void configure(GrpcSecurity builder) throws Exception {

    }
}
