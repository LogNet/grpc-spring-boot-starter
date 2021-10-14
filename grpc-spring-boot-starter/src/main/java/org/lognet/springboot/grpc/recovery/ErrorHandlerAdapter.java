package org.lognet.springboot.grpc.recovery;

import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcErrorHandler;

import java.util.Optional;

@Slf4j
public class ErrorHandlerAdapter {

        private final Optional<GRpcErrorHandler> errorHandler;

        public ErrorHandlerAdapter(Optional<GRpcErrorHandler> errorHandler) {
            this.errorHandler = errorHandler;
        }

        protected Status handle(Exception e,Status status, GRpcExceptionScope scope){

            if(errorHandler.isPresent()){

                return  errorHandler.get().handle(
                        scope.getRequestOrResponse(),
                        status,
                        e,
                        scope.getCallHeaders(),
                        scope.getResponseHeaders());
            }else {
                log.error("Got error with status {} ", status.getCode().name(), e);
                return status.withDescription(e.getMessage());
            }
        }

}
