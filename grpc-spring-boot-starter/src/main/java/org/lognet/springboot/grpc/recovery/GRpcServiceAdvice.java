package org.lognet.springboot.grpc.recovery;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Type-level annotation to register the annotated type as global error handler advice.
 * <p> Use {@link GRpcExceptionHandler} method-level annotation to define  handler method for specific Exception type.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface GRpcServiceAdvice {

}
