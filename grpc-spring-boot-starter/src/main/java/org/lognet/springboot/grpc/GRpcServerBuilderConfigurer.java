package org.lognet.springboot.grpc;

import io.grpc.BindableService;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by 310242212 on 21-Dec-16.
 */
public class GRpcServerBuilderConfigurer {
    public void configure(ServerBuilder<?> serverBuilder){

    }

    public Collection<ServerInterceptor> getServerInterceptors(BindableService srv) {
        return Collections.emptyList();
    }
}
