package org.lognet.springboot.grpc.security;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.lognet.springboot.grpc.GRpcErrorHandler;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.lognet.springboot.grpc.autoconfigure.ConditionalOnMissingErrorHandler;
import org.lognet.springboot.grpc.recovery.ErrorHandlerAdapter;
import org.lognet.springboot.grpc.recovery.GRpcExceptionHandler;
import org.lognet.springboot.grpc.recovery.GRpcExceptionScope;
import org.lognet.springboot.grpc.recovery.GRpcServiceAdvice;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.intercept.aopalliance.MethodSecurityInterceptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collection;
import java.util.Optional;



@Configuration
public class GrpcSecurityConfiguration {
    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private ObjectPostProcessor<Object> objectObjectPostProcessor;

    @SuppressWarnings({ "rawtypes" })
    private Collection<GrpcSecurityConfigurer> grpcSecurityConfigurers;

    private GrpcSecurity grpcSecurity;

    @Bean
    public static BeanPostProcessor bypassMethodInterceptorForGrpcMethodInvocation(){
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if(bean instanceof MethodSecurityInterceptor){
                    return (MethodInterceptor) invocation -> {
                        if (BindableService.class.isAssignableFrom(invocation.getMethod().getDeclaringClass())){
                            return invocation.proceed();
                        }
                        return ((MethodSecurityInterceptor) bean).invoke(invocation);
                    };
                }
                return bean;
            }
        };
    }

    @ConditionalOnMissingErrorHandler(AccessDeniedException.class)
    @Configuration
    static  class DefaultAccessDeniedErrorHandlerConfig{
        @GRpcServiceAdvice
        @Slf4j
        public static class DefaultAccessDeniedErrorHandler extends ErrorHandlerAdapter {

            public DefaultAccessDeniedErrorHandler(Optional<GRpcErrorHandler> errorHandler) {
                super(errorHandler);
            }

            @GRpcExceptionHandler
            public Status handle(AccessDeniedException e, GRpcExceptionScope scope){
                return handle(e,Status.PERMISSION_DENIED,scope);
            }
        }
    }

    @ConditionalOnMissingErrorHandler(AuthenticationException.class)
    @Configuration
    static  class DefaultAuthErrorHandlerConfig{
        @GRpcServiceAdvice
        @Slf4j
        public static class DefaultAuthErrorHandler  extends ErrorHandlerAdapter {
            public DefaultAuthErrorHandler(Optional<GRpcErrorHandler> errorHandler) {
                super(errorHandler);
            }

            @GRpcExceptionHandler
            public Status handle(AuthenticationException e, GRpcExceptionScope scope){
                return handle(e,Status.UNAUTHENTICATED,scope);
            }
        }
    }

    @Bean
    @ConditionalOnMissingBean(GrpcSecurityConfigurerAdapter.class)
    public GrpcSecurityConfigurerAdapter defaultAdapter(){
        return  new GrpcSecurityConfigurerAdapter() {
        };
    }

    @Bean
    @GRpcGlobalInterceptor
    public ServerInterceptor springGrpcSecurityInterceptor() throws Exception   {
        boolean hasConfigurers = grpcSecurityConfigurers != null && !grpcSecurityConfigurers.isEmpty();
        if (!hasConfigurers) {
            GrpcSecurityConfigurerAdapter adapter = objectObjectPostProcessor.postProcess(new GrpcSecurityConfigurerAdapter() {
                    });
            grpcSecurity.apply(adapter);
        }
        return grpcSecurity.build();


    }

    @Bean
    public BasicAuthSchemeSelector basicAuthSchemeSelector() {
        return new BasicAuthSchemeSelector();
    }

    @Bean
    @ConditionalOnClass(name = {
            "org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken",
            "org.springframework.security.oauth2.core.OAuth2AuthenticationException"})
    public BearerTokenAuthSchemeSelector bearerTokenAuthSchemeSelector() {
        return new BearerTokenAuthSchemeSelector();
    }

    @Configuration
    @ConditionalOnClass(AuthenticationManager.class)
    @ConditionalOnBean(ObjectPostProcessor.class)
    @ConditionalOnMissingBean(value = { AuthenticationManager.class, AuthenticationProvider.class, UserDetailsService.class,
            AuthenticationManagerResolver.class }, type = "org.springframework.security.oauth2.jwt.JwtDecoder")
    static class DefaultUserDetailsServiceAutoConfiguration extends UserDetailsServiceAutoConfiguration {}

    @Autowired(required = false)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setFilterChainProxySecurityConfigurer(ObjectPostProcessor<Object> objectPostProcessor) throws Exception {

        grpcSecurity = objectPostProcessor.postProcess(new GrpcSecurity(objectPostProcessor));

        grpcSecurityConfigurers =  beanFactory.getBeansOfType(GrpcSecurityConfigurer.class).values();

        for(GrpcSecurityConfigurer configurer: grpcSecurityConfigurers){
            grpcSecurity.apply(configurer);
        }
    }

}
