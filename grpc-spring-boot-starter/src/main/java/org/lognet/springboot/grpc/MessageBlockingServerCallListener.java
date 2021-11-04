package org.lognet.springboot.grpc;

import io.grpc.ForwardingServerCallListener;
import io.grpc.ServerCall;




public class MessageBlockingServerCallListener<R> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<R> {


    private volatile boolean messageBlocked = false;


    public MessageBlockingServerCallListener(ServerCall.Listener<R> delegate) {
        super(delegate);
    }

    @Override
    public void onHalfClose() {
        // If the message was blocked, downstream never had a chance to react to it. Hence, the half-close signal would look like
        // an error to them. So we do not propagate the signal in that case.
        if (!messageBlocked) {
            super.onHalfClose();
        }
    }

    protected void blockMessage() {
        messageBlocked = true;
    }
}
