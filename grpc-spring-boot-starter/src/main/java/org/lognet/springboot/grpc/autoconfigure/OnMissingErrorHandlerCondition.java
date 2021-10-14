package org.lognet.springboot.grpc.autoconfigure;

import org.lognet.springboot.grpc.recovery.GRpcExceptionHandler;
import org.lognet.springboot.grpc.recovery.GRpcServiceAdvice;
import org.lognet.springboot.grpc.recovery.HandlerMethod;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Optional;

public class OnMissingErrorHandlerCondition extends SpringBootCondition {


    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Class<? extends Throwable> exc = (Class<? extends Throwable>) metadata
                .getAnnotationAttributes(ConditionalOnMissingErrorHandler.class.getName())
                .get("value");

        ReflectionUtils.MethodFilter f = method -> AnnotatedElementUtils.hasAnnotation(method, GRpcExceptionHandler.class);
        for(String adviceBeanName:context.getBeanFactory().getBeanNamesForAnnotation(GRpcServiceAdvice.class)){
            final String beanClassName = context.getBeanFactory().getBeanDefinition(adviceBeanName)
                    .getBeanClassName();

            try {
                for (Method method : MethodIntrospector.selectMethods(Class.forName(beanClassName), f)) {
                    final Optional<Class<? extends Throwable>> handledException = HandlerMethod.getHandledException(method, false);
                    if(handledException.isPresent() && handledException.get().isAssignableFrom(exc)){
                        return ConditionOutcome.noMatch(String.format("Found %s handler at %s.%s",
                                handledException.get().getName(),
                                beanClassName,
                                method.getName()
                                ));
                    }
                }
            } catch (ClassNotFoundException e) {
                throw  new IllegalStateException(e);
            }
        };

        return ConditionOutcome.match();
    }
}
