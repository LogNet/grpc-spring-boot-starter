package org.lognet.springboot.grpc;

import io.grpc.ServerBuilder;

public interface GRpcServerBuilderConfigurer {

    void configure(ServerBuilder<?> serverBuilder);

}
