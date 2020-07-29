package org.lognet.springboot.grpc.security;

import io.grpc.ServerInterceptor;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.ObjectPostProcessor;

import java.util.Collection;

@Configuration
public class GrpcSecurityConfiguration {
    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private ObjectPostProcessor<Object> objectObjectPostProcessor;

    @SuppressWarnings({ "rawtypes" })
    private Collection<GrpcSecurityConfigurer> grpcSecurityConfigurers;

    private GrpcSecurity grpcSecurity;


    public GrpcSecurityConfiguration() {
    }

    @Bean
    @GRpcGlobalInterceptor
    public ServerInterceptor springGrpcSecurityInterceptor() throws Exception   {
        boolean hasConfigurers = grpcSecurityConfigurers != null && !grpcSecurityConfigurers.isEmpty();
        if (!hasConfigurers) {
            GrpcSecurityConfigurerAdapter adapter = objectObjectPostProcessor
                    .postProcess(new GrpcSecurityConfigurerAdapter() {
                    });
            grpcSecurity.apply(adapter);
        }
        return grpcSecurity.build();


    }


    @Autowired(required = false)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setFilterChainProxySecurityConfigurer(
            ObjectPostProcessor<Object> objectPostProcessor)
            throws Exception {

        grpcSecurity = objectPostProcessor.postProcess(new GrpcSecurity(objectPostProcessor));
//        if (debugEnabled != null) {
//            webSecurity.debug(debugEnabled);
//        }

        final Collection<GrpcSecurityConfigurer> configurers = beanFactory
                .getBeansOfType(GrpcSecurityConfigurer.class).values();

        // webSecurityConfigurers.sort(WebSecurityConfiguration.AnnotationAwareOrderComparator.INSTANCE);

//        Integer previousOrder = null;
//        Object previousConfig = null;
//        for (SecurityConfigurer<Filter, WebSecurity> config : webSecurityConfigurers) {
//            Integer order = WebSecurityConfiguration.AnnotationAwareOrderComparator.lookupOrder(config);
//            if (previousOrder != null && previousOrder.equals(order)) {
//                throw new IllegalStateException(
//                        "@Order on WebSecurityConfigurers must be unique. Order of "
//                                + order + " was already used on " + previousConfig + ", so it cannot be used on "
//                                + config + " too.");
//            }
//            previousOrder = order;
//            previousConfig = config;
//        }
        for (GrpcSecurityConfigurer   configurer : configurers) {
            grpcSecurity.apply(configurer);
        }
        this.grpcSecurityConfigurers = configurers;
    }

}
