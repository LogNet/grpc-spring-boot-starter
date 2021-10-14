package org.lognet.springboot.grpc.recovery;

import io.grpc.Status;
import lombok.Getter;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;


@Getter
public class HandlerMethod {
    private final Object target;
    private final Method method;
    private final Class<? extends Throwable> exceptionType;


    private HandlerMethod() {
        throw new UnsupportedOperationException();
    }
    private HandlerMethod(Object target, Method method) {

        this.target = target;
        this.method = method;
        this.exceptionType = getHandledException(method,true).get();


    }

    public static HandlerMethod create(Object target, Method method) {
        return  new HandlerMethod(target,method);
    }

    public Status invoke(Throwable e,GRpcExceptionScope  scope) throws InvocationTargetException, IllegalAccessException {
        ReflectionUtils.makeAccessible(method);
        return (Status) ReflectionUtils.invokeMethod(method,target,e,scope);
    }

    public static Optional<Class<? extends Throwable>> getHandledException(Method method, boolean throwsException){
        try {
            Assert.state(2 == method.getParameterCount(), () -> "There should be exactly 2 parameters on method " + method);

            final Class<?>[] parameterTypes = method.getParameterTypes();
            Assert.state(Throwable.class.isAssignableFrom(parameterTypes[0]), () -> "First parameter of method " + method + " has to be of type" + Throwable.class.getName());
            Assert.state(GRpcExceptionScope.class.isAssignableFrom(parameterTypes[1]), () -> "Second parameter of method " + method + " has to be of type" + GRpcExceptionScope.class.getName());

            Assert.state(Status.class.isAssignableFrom(method.getReturnType()), () -> "Return type of method " + method + " has to be " + Status.class.getName());
            return Optional.of((Class<? extends Throwable>) parameterTypes[0]);
        }catch (IllegalArgumentException e){
            if(throwsException){
                throw e;
            }
            return Optional.empty();
        }

    }

}
