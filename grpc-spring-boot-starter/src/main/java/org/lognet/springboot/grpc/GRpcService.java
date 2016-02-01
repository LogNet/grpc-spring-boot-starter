package org.lognet.springboot.grpc;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;

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
