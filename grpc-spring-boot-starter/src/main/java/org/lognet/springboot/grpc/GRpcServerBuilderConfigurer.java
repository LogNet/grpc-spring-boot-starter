package org.lognet.springboot.grpc;

import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

/**
 * Created by 310242212 on 21-Dec-16.
 */
public class GRpcServerBuilderConfigurer {
    public void configure(ServerBuilder<?> serverBuilder){

    }

    public void configureInProcessServerBuilder(InProcessServerBuilder inProcessServerBuilder) {}

}
