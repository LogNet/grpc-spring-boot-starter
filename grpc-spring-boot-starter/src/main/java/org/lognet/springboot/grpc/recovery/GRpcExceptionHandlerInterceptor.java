package org.lognet.springboot.grpc.recovery;

import io.grpc.Context;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.lognet.springboot.grpc.FailureHandlingSupport;
import org.lognet.springboot.grpc.MessageBlockingServerCallListener;
import org.springframework.core.Ordered;

import java.util.concurrent.atomic.AtomicBoolean;

public class GRpcExceptionHandlerInterceptor implements ServerInterceptor, Ordered {
    private static final Context.Key<AtomicBoolean> CALL_IS_CLOSED = Context.key("CALL_IS_CLOSED");
    private final GRpcExceptionHandlerMethodResolver methodResolver;
    private final FailureHandlingSupport failureHandlingSupport;


    public GRpcExceptionHandlerInterceptor(GRpcExceptionHandlerMethodResolver methodResolver, FailureHandlingSupport failureHandlingSupport) {
        this.methodResolver = methodResolver;
        this.failureHandlingSupport = failureHandlingSupport;
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
        return Ordered.HIGHEST_PRECEDENCE;
    }


}
