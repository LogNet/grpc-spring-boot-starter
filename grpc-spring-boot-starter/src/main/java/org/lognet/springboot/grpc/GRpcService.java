package org.lognet.springboot.grpc;

import io.grpc.ServerInterceptor;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * Marks the annotated class to be registered as grpc-service bean;
 *
 * @author Furer Alexander
 * @since 0.0.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface GRpcService {
    Class<? extends ServerInterceptor>[] interceptors() default {};

    boolean applyGlobalInterceptors() default true;

    /**
     * @return protoc-generated class that creates {@link io.grpc.ServerServiceDefinition} via static <code>bindService</code> function.
     */
    Class<?> grpcServiceOuterClass();

    String serviceName();

    String version() default "1.0.0-SNAPSHOT";
}
