package org.lognet.springboot.grpc.recovery;

import io.grpc.Status;
import lombok.Getter;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


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

        Assert.state(2 == method.getParameterCount(), () -> "There should be exactly 2 parameters on method " + method);

        final Class<?>[] parameterTypes = method.getParameterTypes();
        Assert.state(Throwable.class.isAssignableFrom(parameterTypes[0]), () -> "First parameter of method " + method + " has to be of type" + Throwable.class.getName());
        Assert.state(GRpcExceptionScope.class.isAssignableFrom(parameterTypes[1]), () -> "Second parameter of method " + method + " has to be of type" + GRpcExceptionScope.class.getName());

        Assert.state(Status.class.isAssignableFrom(method.getReturnType()),()->"Return type of method " + method + " has to be " + Status.class.getName());
        exceptionType = (Class<? extends Throwable>) parameterTypes[0];

    }

    public static HandlerMethod create(Object target, Method method) {
        return  new HandlerMethod(target,method);
    }

    public Status invoke(Throwable e,GRpcExceptionScope  scope) throws InvocationTargetException, IllegalAccessException {
        ReflectionUtils.makeAccessible(method);
        return (Status) ReflectionUtils.invokeMethod(method,target,e,scope);
    }

}
