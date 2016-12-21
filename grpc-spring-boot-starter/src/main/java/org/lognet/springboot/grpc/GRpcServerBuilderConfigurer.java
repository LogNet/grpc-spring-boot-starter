package org.lognet.springboot.grpc;

import io.grpc.ServerBuilder;

/**
 * Created by 310242212 on 21-Dec-16.
 */
public class GRpcServerBuilderConfigurer {
    public ServerBuilder<?> configure(ServerBuilder<?> serverBuilder){
        return serverBuilder;
    }
}
