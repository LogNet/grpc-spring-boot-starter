package org.lognet.springboot.grpc.security;

import lombok.Getter;
import org.lognet.springboot.grpc.GRpcServicesRegistry;
import org.lognet.springboot.grpc.security.jwt.JwtAuthProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

public abstract class GrpcSecurityConfigurerAdapter extends GrpcSecurityConfigurer<GrpcSecurity> {

    private AuthenticationConfiguration authenticationConfiguration;
    private AuthenticationManagerBuilder authenticationManagerBuilder;
    @Getter
    private ApplicationContext context;

    protected GrpcSecurityConfigurerAdapter() {
    }



    @Autowired
    public void setApplicationContext(ApplicationContext context) throws Exception {


        ObjectPostProcessor<Object> objectPostProcessor = context.getBean("objectPostProcessor", ObjectPostProcessor.class);
        this.authenticationConfiguration = context.getBean(AuthenticationConfiguration.class);

        authenticationManagerBuilder = authenticationConfiguration
                .authenticationManagerBuilder(objectPostProcessor, context)
                .parentAuthenticationManager(authenticationConfiguration.getAuthenticationManager());

        this.context = context;
    }

    @Override
    public void init(GrpcSecurity builder) throws Exception {
        builder.apply(new GrpcServiceAuthorizationConfigurer(builder.getApplicationContext().getBean(GRpcServicesRegistry.class)));
        builder.setSharedObject(AuthenticationManagerBuilder.class, authenticationManagerBuilder);
        final AuthenticationSchemeService authenticationSchemeService = new AuthenticationSchemeService();


        context.getBeansOfType(AuthenticationSchemeSelector.class)
                .values()
                .forEach(authenticationSchemeService::register);

        builder.setSharedObject(AuthenticationSchemeService.class, authenticationSchemeService);

    }



    @Override
    public void configure(GrpcSecurity builder) throws Exception {
        try {
            final Class<?> jwtDecoderClass = Class.forName("org.springframework.security.oauth2.jwt.JwtDecoder");
            final String[] beanNames = context.getBeanNamesForType(jwtDecoderClass);
            if (1 == beanNames.length) {
                builder.authenticationProvider(JwtAuthProviderFactory.forAuthorities(context.getBean(beanNames[0], JwtDecoder.class)));
            }
        } catch (ClassNotFoundException e) {
            //swallow
        }
        builder.authorizeRequests()
                .withSecuredAnnotation();
    }
}
