package org.lognet.springboot.grpc.recovery;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import org.lognet.springboot.grpc.FailureHandlingServerInterceptor;
import org.springframework.core.Ordered;

import java.util.Optional;

public class GRpcExceptionHandlerInterceptor implements FailureHandlingServerInterceptor, Ordered {

    private final GRpcExceptionHandlerMethodResolver methodResolver;

    public GRpcExceptionHandlerInterceptor(GRpcExceptionHandlerMethodResolver methodResolver) {
        this.methodResolver = methodResolver;
    }


    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        if (!methodResolver.hasErrorHandlers()) {
            return next.startCall(call, headers);
        }
        return new MessageBlockingServerCallListener<ReqT>(next.startCall(call, headers)) {
            private ReqT request;
            private volatile boolean closed = false;

            @Override
            public void onMessage(ReqT message) {
                try {
                    request = message;
                    super.onMessage(message);
                } catch (RuntimeException e) {
                    blockMessage();
                    fail(e);
                }
            }

            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (RuntimeException e) {
                    fail(e);
                }
            }


            public void fail(RuntimeException e) throws RuntimeException {
                if (!closed) {
                    closed = true;
                    final Optional<HandlerMethod> handlerMethod = methodResolver.resolveMethodByThrowable(call.getMethodDescriptor().getServiceName(), e);
                    if (handlerMethod.isPresent()) {
                        final GRpcExceptionScope exceptionScope = GRpcExceptionScope.builder()
                                .callHeaders(headers)
                                .methodCallAttributes(call.getAttributes())
                                .methodDescriptor(call.getMethodDescriptor())
                                .request(request)
                                .build();
                        closeCall(call, GRpcExceptionHandlerMethodResolver.unwrap(e), exceptionScope, handlerMethod.get());

                    } else {
                        throw e;
                    }
                }


            }
        };

    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }


}
