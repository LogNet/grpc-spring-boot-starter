package org.lognet.springboot.grpc;

import io.grpc.Metadata;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        log.error("Got error with status {} ",status.getCode().name(),exception);
        return status.withDescription(exception.getMessage());
    }
}
