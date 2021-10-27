package org.lognet.springboot.grpc.recovery;

import org.lognet.springboot.grpc.GRpcServicesRegistry;
import org.springframework.core.ExceptionDepthComparator;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GRpcExceptionHandlerMethodResolver {


    private final Map<Class<? extends Throwable>, HandlerMethod> mappedHandlers = new HashMap<>(16);
    private final Map<Class<? extends Throwable>, HandlerMethod> exceptionLookupCache = new ConcurrentReferenceHashMap<>(16);
    private Map<String, GRpcExceptionHandlerMethodResolver> privateResolvers;

    public GRpcExceptionHandlerMethodResolver(GRpcServicesRegistry registry, Collection<Object> advices) {
        this(advices);

        privateResolvers = registry.getServiceNameToServiceBeanMap()
                .entrySet()
                .stream()
                .peek(e->{
                    if(null!=AnnotationUtils.findAnnotation(e.getValue().getClass(),GRpcServiceAdvice.class)){
                        throw new IllegalStateException(String.format("Service %s should NOT be annotated with %s",
                                e.getValue().getClass().getName(),
                                GRpcServiceAdvice.class.getName()
                                ));
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new GRpcExceptionHandlerMethodResolver(Collections.singleton(e.getValue()))));
    }

    public boolean hasErrorHandlers() {
        return !mappedHandlers.isEmpty();
    }

    private GRpcExceptionHandlerMethodResolver(Collection<Object> advices) {


        ReflectionUtils.MethodFilter f = method -> AnnotatedElementUtils.hasAnnotation(method, GRpcExceptionHandler.class);
        for (Object advice : advices) {
            for (Method method : MethodIntrospector.selectMethods(advice.getClass(), f)) {
                final HandlerMethod handlerMethod = HandlerMethod.create(advice, method);
                final Class<? extends Throwable> exceptionType = handlerMethod.getExceptionType();
                HandlerMethod oldHandler = this.mappedHandlers.put(exceptionType, handlerMethod);
                if (null!=oldHandler) {
                    throw new IllegalStateException("Ambiguous @GRpcExceptionHandler method mapped for [" +
                            exceptionType + "]: {" + oldHandler.getMethod() + ", " + handlerMethod.getMethod() + "}");
                }
            }
        }
    }


    public Optional<HandlerMethod> resolveMethodByThrowable(String grpcServiceName, Throwable exc) {
        if(null==exc){
            return Optional.empty();
        }
        Throwable exception = GRpcRuntimeExceptionWrapper.unwrap(exc);

        Optional<HandlerMethod> method = Optional.ofNullable(privateResolvers)
                .map(r -> r.get(grpcServiceName))
                .flatMap(r -> r.resolveMethodByThrowable(grpcServiceName, exception));

        if (method.isPresent()) {
            return method;
        }
        method = resolveMethodByExceptionType(exception.getClass());
        if (method.isPresent()) {
            return method;
        } else {
            Throwable cause = exception.getCause();
            if (cause != null) {
                method = resolveMethodByThrowable(grpcServiceName, cause);
            }
        }
        return method;

    }


    private Optional<HandlerMethod> resolveMethodByExceptionType(Class<? extends Throwable> exceptionType) {

        HandlerMethod method = this.exceptionLookupCache.get(exceptionType);

        if (null != method) {
            return Optional.of(method);
        } else {
            method = getMappedMethod(exceptionType);
            if (null != method) {
                this.exceptionLookupCache.put(exceptionType, method);
                return Optional.of(method);
            }
        }
        return Optional.empty();

    }


    private HandlerMethod getMappedMethod(Class<? extends Throwable> exceptionType) {
        List<Class<? extends Throwable>> matches = new ArrayList<>();
        for (Class<? extends Throwable> mappedException : this.mappedHandlers.keySet()) {
            if (mappedException.isAssignableFrom(exceptionType)) {
                matches.add(mappedException);
            }
        }
        if (!matches.isEmpty()) {
            if (matches.size() > 1) {
                matches.sort(new ExceptionDepthComparator(exceptionType));
            }
            return this.mappedHandlers.get(matches.get(0));
        }
        return null;
    }



}
