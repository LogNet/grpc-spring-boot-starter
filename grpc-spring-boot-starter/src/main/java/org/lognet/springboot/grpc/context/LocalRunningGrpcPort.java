package org.lognet.springboot.grpc.context;

import org.springframework.beans.factory.annotation.Value;

import java.lang.annotation.*;

@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
        ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Value("${" + LocalRunningGrpcPort.propertyName + "}")
public @interface LocalRunningGrpcPort {
    String propertyName = "local.grpc.port";
}
