package org.lognet.springboot.grpc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Service;

/**
 *
 * Marks the annotated class to be registered as grpc-service bean;
 * @author  Furer Alexander
 * @since 0.0.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface GRpcService {
    /**
     *
     * @return protoc-generated class that creates {@link io.grpc.ServerServiceDefinition} via static <code>bindService</code> function.
     */
    Class<?> grpcServiceOuterClass();
}
