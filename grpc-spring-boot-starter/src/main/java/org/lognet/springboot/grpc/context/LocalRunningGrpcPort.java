package org.lognet.springboot.grpc.context;

import org.springframework.beans.factory.annotation.Value;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Value("${grpc.port !=0 ? ${grpc.port}:${local.grpc.port}}")
public @interface LocalRunningGrpcPort {
}
