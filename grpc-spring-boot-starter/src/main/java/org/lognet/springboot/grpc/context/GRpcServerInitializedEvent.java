package org.lognet.springboot.grpc.context;

import io.grpc.Server;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

public class GRpcServerInitializedEvent extends ApplicationEvent {
    private final Server server;

    public GRpcServerInitializedEvent(ApplicationContext context,Server server) {
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
