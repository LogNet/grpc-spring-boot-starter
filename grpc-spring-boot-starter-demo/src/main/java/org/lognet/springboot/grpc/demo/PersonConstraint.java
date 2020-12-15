package org.lognet.springboot.grpc.demo;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({  TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = PersonValidator.class)
@Documented
public @interface PersonConstraint {

    String message() default "{person.validation.message}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
