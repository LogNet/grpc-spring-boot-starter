package org.lognet.springboot.grpc.recovery;

import io.grpc.*;
import org.lognet.springboot.grpc.FailureHandlingSupport;
import org.lognet.springboot.grpc.MessageBlockingServerCallListener;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.lognet.springboot.grpc.security.SecurityInterceptor;
import org.springframework.core.Ordered;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class GRpcExceptionHandlerInterceptor implements ServerInterceptor, Ordered {

    public static final Context.Key<AtomicBoolean> EXCEPTION_HANDLED = Context.key("org.lognet.springboot.grpc.recovery.EXCEPTION_HANDLED");
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
        final AtomicBoolean exceptionHandled = new AtomicBoolean(false);


        if (!methodResolver.hasErrorHandlers()) {
            return next.startCall(call, headers);
        }


        final ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> errorHandlingCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {

            @Override
            public void close(Status status, Metadata trailers) {
                // prevent close from being  invoked twice
                //  (like from Reactive service from different thread , close method invoked directly )
                Boolean handled = Optional.ofNullable(EXCEPTION_HANDLED.get())
                        .map(AtomicBoolean::get)
                        .orElse(!exceptionHandled.compareAndSet(false, true));
                if(null != status.getCause() && !handled){
                        failureHandlingSupport.closeCall(new GRpcRuntimeExceptionWrapper(status.getCause()), this, trailers);
                }

                if (callIsClosed.compareAndSet(false, true)) {
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


        Context context = Context
                .current()
                .withValue(EXCEPTION_HANDLED, new AtomicBoolean(false));
        return Contexts.interceptCall(context, errorHandlingCall, headers, new ServerCallHandler<ReqT, RespT>() {
            @Override
            public ServerCall.Listener<ReqT> startCall(ServerCall<ReqT, RespT> call, Metadata headers) {
                final ServerCall.Listener<ReqT> listener;
                try {

                    listener = next.startCall(call, headers);
                } catch (RuntimeException e) {
                    failureHandlingSupport.closeCall(e, call, headers);
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
                            failureHandlingSupport.closeCall(e, call, headers, b -> b.request(request));
                        }
                    }

                    @Override
                    public void onHalfClose() {
                        try {
                            if (!callIsClosed.get()) {
                                super.onHalfClose();
                            }
                        } catch (RuntimeException e) {
                            failureHandlingSupport.closeCall(e, call, headers, b -> b.request(request));
                        }
                    }

                };
            }
        });


    }


    @Override
    public int getOrder() {
        return order;
    }


}
