package org.lognet.springboot.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import org.lognet.springboot.grpc.recovery.GRpcExceptionHandlerMethodResolver;
import org.lognet.springboot.grpc.recovery.GRpcExceptionScope;
import org.lognet.springboot.grpc.recovery.GRpcRuntimeExceptionWrapper;
import org.lognet.springboot.grpc.recovery.HandlerMethod;

import java.util.Optional;
import java.util.function.Consumer;

public class FailureHandlingSupport {

    private final GRpcExceptionHandlerMethodResolver methodResolver;

    public FailureHandlingSupport(GRpcExceptionHandlerMethodResolver methodResolver) {
        this.methodResolver = methodResolver;
    }

    public void closeCall(RuntimeException e, ServerCall<?, ?> call, Metadata headers, Consumer<GRpcExceptionScope.GRpcExceptionScopeBuilder> customizer) throws RuntimeException {


            final Optional<HandlerMethod> handlerMethod = methodResolver.resolveMethodByThrowable(call.getMethodDescriptor().getServiceName(), e);
            if (handlerMethod.isPresent()) {
                final GRpcExceptionScope.GRpcExceptionScopeBuilder exceptionScopeBuilder = GRpcExceptionScope.builder()
                        .callHeaders(headers)
                        .methodCallAttributes(call.getAttributes())
                        .methodDescriptor(call.getMethodDescriptor())
                        .hint(GRpcRuntimeExceptionWrapper.getHint(e));
                customizer.accept(exceptionScopeBuilder);

                final GRpcExceptionScope excScope = exceptionScopeBuilder.build();

                final HandlerMethod handler = handlerMethod.get();


                Status statusToSend = Status.INTERNAL;
                try {
                    statusToSend = handler.invoke(GRpcRuntimeExceptionWrapper.unwrap(e),excScope );
                }catch (Exception handlerException){

                    org.slf4j.LoggerFactory.getLogger(this.getClass())
                            .error("Caught exception while executing handler method {}, returning {} status.",
                                    handler.getMethod(),
                                    statusToSend,
                                    handlerException);

                }
                call.close(statusToSend, excScope.getResponseHeaders());


            } else {
                call.close(Status.INTERNAL,new Metadata());
            }

    }



}
