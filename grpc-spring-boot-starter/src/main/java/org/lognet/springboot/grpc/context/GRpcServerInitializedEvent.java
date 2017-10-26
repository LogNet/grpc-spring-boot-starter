package org.lognet.springboot.grpc.context;

import io.grpc.Server;
import org.springframework.context.ApplicationEvent;

public class GRpcServerInitializedEvent extends ApplicationEvent {
    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public GRpcServerInitializedEvent(Server source) {
        super(source);
    }
    public Server getServer(){
        return (Server) getSource();
    }
}
