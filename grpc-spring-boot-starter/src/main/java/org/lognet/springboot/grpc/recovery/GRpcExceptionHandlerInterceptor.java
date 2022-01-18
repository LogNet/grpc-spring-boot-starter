package org.lognet.springboot.grpc.recovery;

import io.grpc.*;
import org.lognet.springboot.grpc.FailureHandlingSupport;
import org.lognet.springboot.grpc.MessageBlockingServerCallListener;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.springframework.core.Ordered;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class GRpcExceptionHandlerInterceptor implements ServerInterceptor, Ordered {

    private final GRpcExceptionHandlerMethodResolver methodResolver;
    private final FailureHandlingSupport failureHandlingSupport;

    private Integer order;

    public GRpcExceptionHandlerInterceptor(GRpcExceptionHandlerMethodResolver methodResolver, FailureHandlingSupport failureHandlingSupport, GRpcServerProperties serverProperties) {
        this.methodResolver = methodResolver;
        this.failureHandlingSupport = failureHandlingSupport;
        this.order = Optional.ofNullable(serverProperties.getRecovery())
                .map(GRpcServerProperties.RecoveryProperties::getInterceptorOrder)
                .orElse(Ordered.HIGHEST_PRECEDENCE);
    }


    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        final AtomicBoolean callIsClosed = new AtomicBoolean(false);


        if (!methodResolver.hasErrorHandlers()) {
            return next.startCall(call, headers);
        }


        final ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> errorHandlingCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {

            @Override
            public void close(Status status, Metadata trailers) {
                if( callIsClosed.compareAndSet(false,true)){
                    super.close(status, trailers);
                }

            }

            @Override
            public void sendMessage(RespT message) {
                try {
                    super.sendMessage(message);
                } catch (RuntimeException e) {
                    failureHandlingSupport.closeCall(e, this, headers, b -> b.response(message));
                }
            }
        };
        final ServerCall.Listener<ReqT> listener;
        try {

            listener = next.startCall( errorHandlingCall, headers);
        } catch (RuntimeException e) {
            failureHandlingSupport.closeCall(e, errorHandlingCall, headers);
            return new ServerCall.Listener<ReqT>() {

            };
        }
        return new MessageBlockingServerCallListener<ReqT>(listener) {
            private ReqT request;

            @Override
            public void onMessage(ReqT message) {
                try {
                    request = message;
                    super.onMessage(message);
                } catch (RuntimeException e) {
                    blockMessage();
                    failureHandlingSupport.closeCall(e, errorHandlingCall, headers, b -> b.request(request));
                }
            }

            @Override
            public void onHalfClose() {
                try {
                    if(!callIsClosed.get()) {
                        super.onHalfClose();
                    }
                } catch (RuntimeException e) {
                    failureHandlingSupport.closeCall(e, errorHandlingCall, headers, b -> b.request(request));
                }
            }

        };


    }


    @Override
    public int getOrder() {
        return order;
    }


}
