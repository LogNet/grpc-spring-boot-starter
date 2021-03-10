package org.lognet.springboot.grpc;

import io.grpc.Metadata;
import io.grpc.Status;

public class GRpcErrorHandler {

    /**
     *
     * @param status
     * @param exception
     * @param requestHeaders
     * @param responseHeaders - to be filled by implementor with trails meant to be sent to client
     * @return
     */
    public Status handle(Status status, Exception exception, Metadata requestHeaders, Metadata responseHeaders) {
        return handle(null,status,exception,requestHeaders,responseHeaders);
    }

    /**
     *
     * @param message - request message
     * @param status
     * @param exception
     * @param requestHeaders
     * @param responseHeaders -  to be filled by implementor with trails meant to be sent to client
     * @return
     */
    public Status handle(Object message,Status status, Exception exception, Metadata requestHeaders, Metadata responseHeaders) {
        return status.withDescription(exception.getMessage());
    }
}
