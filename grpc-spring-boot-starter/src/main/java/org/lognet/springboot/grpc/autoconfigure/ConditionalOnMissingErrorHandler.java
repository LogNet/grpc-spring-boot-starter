package org.lognet.springboot.grpc.autoconfigure;


import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE , ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnMissingErrorHandlerCondition.class)
public @interface ConditionalOnMissingErrorHandler {
    Class<? extends Throwable> value();
}
