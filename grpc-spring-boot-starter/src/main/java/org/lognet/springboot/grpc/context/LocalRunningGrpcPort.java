package org.lognet.springboot.grpc.context;

import org.springframework.beans.factory.annotation.Value;

import java.lang.annotation.*;

@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
        ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Value("#{@'grpc-org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties'.getRunningPort()}")
public @interface LocalRunningGrpcPort {
}
