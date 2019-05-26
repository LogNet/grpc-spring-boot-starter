package org.lognet.springboot.grpc.context;

import io.grpc.Server;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

public abstract class GrpcServerEvent extends ApplicationEvent {
    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public GrpcServerEvent(Object source) {
        super(source);
    }

    public abstract Server getServer();
    public abstract ApplicationContext getApplicationContext();
}
