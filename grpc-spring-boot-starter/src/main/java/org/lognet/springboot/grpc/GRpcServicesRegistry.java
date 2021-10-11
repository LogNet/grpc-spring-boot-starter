package org.lognet.springboot.grpc;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GRpcServicesRegistry implements InitializingBean , ApplicationContextAware {
    private ApplicationContext applicationContext;


    private    Map<String, BindableService> beanNameToServiceBean;

    private    Map<String, BindableService> serviceNameToServiceBean;

    private    Collection<ServerInterceptor> grpcGlobalInterceptors;



    /**
     *
     * @return service name to grpc service bean
     */
    public Map<String, BindableService> getServiceNameToServiceBeanMap() {
        return serviceNameToServiceBean;
    }

    /**
     *
     * @return spring bean name to grpc service bean
     */
    public Map<String, BindableService> getBeanNameToServiceBeanMap() {
        return beanNameToServiceBean;
    }

    Collection<ServerInterceptor> getGlobalInterceptors() {

        return grpcGlobalInterceptors;
    }

    private <T> Map<String,T> getBeanNamesByTypeWithAnnotation(Class<? extends Annotation> annotationType, Class<T> beanType) {

        return applicationContext.getBeansWithAnnotation(annotationType)
                .entrySet()
                .stream()
                .filter(e-> beanType.isInstance(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey,e->beanType.cast(e.getValue())));

    }

    @Override
    public void afterPropertiesSet() throws Exception {



        beanNameToServiceBean = getBeanNamesByTypeWithAnnotation(GRpcService.class, BindableService.class);

        serviceNameToServiceBean = beanNameToServiceBean
                .values()
                .stream()
                .collect(Collectors.toMap(s->s.bindService().getServiceDescriptor().getName(),Function.identity()));


        grpcGlobalInterceptors = getBeanNamesByTypeWithAnnotation(GRpcGlobalInterceptor.class, ServerInterceptor.class)
                .values();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext =   applicationContext;
    }
}
