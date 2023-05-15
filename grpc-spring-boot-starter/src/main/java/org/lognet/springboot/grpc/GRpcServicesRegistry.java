package org.lognet.springboot.grpc;

import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import lombok.Builder;
import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.function.SingletonSupplier;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GRpcServicesRegistry implements InitializingBean, ApplicationContextAware {
    @Getter
    @Builder
    public static class GrpcServiceMethod {
        private BindableService service;
        private Method method;

    }

    private ApplicationContext applicationContext;

    private Supplier<Map<String, BindableService>> beanNameToServiceBean;

    private Supplier<Map<String, BindableService>> serviceNameToServiceBean;

    private Supplier<Collection<ServerInterceptor>> grpcGlobalInterceptors;

    private Supplier<Map<MethodDescriptor<?, ?>, GrpcServiceMethod>> descriptorToServiceMethod;

    private Supplier<Map<Method, MethodDescriptor<?, ?>>> methodToDescriptor;


    /**
     * @return service name to grpc service bean
     */
    public Map<String, BindableService> getServiceNameToServiceBeanMap() {
        return serviceNameToServiceBean.get();
    }

    /**
     * @return spring bean name to grpc service bean
     */
    public Map<String, BindableService> getBeanNameToServiceBeanMap() {
        return beanNameToServiceBean.get();
    }

    Collection<ServerInterceptor> getGlobalInterceptors() {

        return grpcGlobalInterceptors.get();
    }

    public GrpcServiceMethod getGrpServiceMethod(MethodDescriptor<?, ?> descriptor) {
        return descriptorToServiceMethod.get().get(descriptor);
    }

    public MethodDescriptor<?, ?> getMethodDescriptor(Method method) {
        return methodToDescriptor.get().get(method);
    }

    private <T> Map<String, T> getBeanNamesByTypeWithAnnotation(Class<? extends Annotation> annotationType, Class<T> beanType) {

        return applicationContext.getBeansWithAnnotation(annotationType)
                .entrySet()
                .stream()
                .filter(e -> beanType.isInstance(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> beanType.cast(e.getValue())));

    }

    @Override
    public void afterPropertiesSet() throws Exception {

        descriptorToServiceMethod = SingletonSupplier.of(this::descriptorToServiceMethod);

        methodToDescriptor = SingletonSupplier.of(() ->
                descriptorToServiceMethod.get()
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(e -> e.getValue().getMethod(), Map.Entry::getKey))
        );
        beanNameToServiceBean = SingletonSupplier.of(() ->
                getBeanNamesByTypeWithAnnotation(GRpcService.class, BindableService.class)
        );


        serviceNameToServiceBean = SingletonSupplier.of(() ->
                beanNameToServiceBean
                        .get()
                        .values()
                        .stream()
                        .collect(Collectors.toMap(s -> s.bindService().getServiceDescriptor().getName(), Function.identity()))
        );

        grpcGlobalInterceptors = SingletonSupplier.of(() ->
                getBeanNamesByTypeWithAnnotation(GRpcGlobalInterceptor.class, ServerInterceptor.class)
                        .values()
        );
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private Map<MethodDescriptor<?, ?>, GrpcServiceMethod> descriptorToServiceMethod() {
        final Map<MethodDescriptor<?, ?>, GrpcServiceMethod> map = new HashMap<>();

        Function<String, ReflectionUtils.MethodFilter> filterFactory = name ->
                method ->
                        method.getName().equalsIgnoreCase(name.replaceAll("_", ""));


        Predicate<Method> firstArgIsMono = m -> "reactor.core.publisher.Mono".equals(m.getParameterTypes()[0].getName());
        Predicate<Method> singleArg = m -> 1 == m.getParameterCount();

        for (BindableService service : getBeanNameToServiceBeanMap().values()) {
            final ServerServiceDefinition serviceDefinition = service.bindService();
            for (MethodDescriptor<?, ?> d : serviceDefinition.getServiceDescriptor().getMethods()) {
                Class<?> abstractBaseClass = service.getClass();
                while (!Modifier.isAbstract(abstractBaseClass.getModifiers())) {
                    abstractBaseClass = abstractBaseClass.getSuperclass();
                }

                final Set<Method> methods = MethodIntrospector
                        .selectMethods(abstractBaseClass, filterFactory.apply(d.getBareMethodName()));


                switch (methods.size()) {
                    case 0:
                        throw new IllegalStateException("Method " + d.getBareMethodName() + "not found in service " + serviceDefinition.getServiceDescriptor().getName());
                    case 1:
                        map.put(d, GrpcServiceMethod.builder()
                                .service(service)
                                .method(methods.iterator().next())
                                .build());
                        break;
                    default:
                        if (2 == methods.size()) {

                            Optional<Method> methodWithMono = methods.stream() // grpcMethod(Mono<Payload> arg)
                                    .filter(singleArg.and(firstArgIsMono))
                                    .findFirst();

                            Optional<Method> methodPure = methods.stream() // grpcMethod(Payload arg)
                                    .filter(singleArg.and(firstArgIsMono.negate()))
                                    .findFirst();

                            Class<?> finalAbstractBaseClass = abstractBaseClass;
                            Boolean typesAreEqual = methodWithMono
                                    .map(m -> ((ParameterizedType) new MethodParameter(m, 0)
                                            .withContainingClass(finalAbstractBaseClass)
                                            .getGenericParameterType())
                                            .getActualTypeArguments()[0]
                                    ).map(t -> t.equals(methodPure.map(m -> m.getParameterTypes()[0]).orElse(null)))
                                    .orElse(false);

                            if (typesAreEqual) {
                                map.put(d, GrpcServiceMethod.builder()
                                        .service(service)
                                        .method(methodWithMono.get())
                                        .build());
                                break;
                            }
                        }
                        throw new IllegalStateException("Ambiguous method " + d.getBareMethodName() + " in service " + serviceDefinition.getServiceDescriptor().getName());


                }


            }
        }
        return Collections.unmodifiableMap(map);
    }
}
