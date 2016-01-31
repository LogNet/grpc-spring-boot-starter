package org.lognet.springboot.grpc;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Created by alexf on 25-Jan-16.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface GRpcService {
    Class<?> grpcClass();
}
