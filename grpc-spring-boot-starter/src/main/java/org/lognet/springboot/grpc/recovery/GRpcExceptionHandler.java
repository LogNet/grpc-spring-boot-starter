package org.lognet.springboot.grpc.recovery;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the annotated method eligible for handling the specific exception type.
 * The signature of the method HAS to be as follows:
 * <br/>
 * {@code Status handlerName(Exception type,GRpcExceptionScope scope)}
 * <br/>, example:
 * <br/>
 * <pre>
 * {@code public io.grpc.Status handlerName(MyCustomException ex,GRpcExceptionScope scope){
 *     ...
 *   }}
 * </pre>
 *
 * @see  GRpcServiceAdvice
 * @see  GRpcRuntimeExceptionWrapper
 * @see  GRpcExceptionScope
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GRpcExceptionHandler {


}
