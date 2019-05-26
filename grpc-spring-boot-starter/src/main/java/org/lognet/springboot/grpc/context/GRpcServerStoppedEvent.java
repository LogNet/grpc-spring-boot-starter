package org.lognet.springboot.grpc.context;

import io.grpc.Server;
import org.springframework.context.ApplicationContext;

public class GRpcServerStoppedEvent extends GrpcServerEvent {
    private Server server;

    public GRpcServerStoppedEvent(ApplicationContext context, Server server) {
        super(context);
        this.server = server;
    }

    public ApplicationContext getApplicationContext(){
        return (ApplicationContext) getSource();
    }
    public Server getServer(){
        return server;
    }
}
