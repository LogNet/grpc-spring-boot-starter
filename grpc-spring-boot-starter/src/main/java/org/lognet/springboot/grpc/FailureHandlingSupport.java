package org.lognet.springboot.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.recovery.GRpcExceptionHandlerMethodResolver;
import org.lognet.springboot.grpc.recovery.GRpcExceptionScope;
import org.lognet.springboot.grpc.recovery.GRpcRuntimeExceptionWrapper;
import org.lognet.springboot.grpc.recovery.HandlerMethod;

import java.util.Optional;
import java.util.function.Consumer;
@Slf4j
public class FailureHandlingSupport {

    private final GRpcExceptionHandlerMethodResolver methodResolver;



    public FailureHandlingSupport(GRpcExceptionHandlerMethodResolver methodResolver) {
        this.methodResolver = methodResolver;
    }




    public void closeCall(RuntimeException e, ServerCall<?, ?> call, Metadata headers) throws RuntimeException {
        closeCall(e,call,headers,null);
    }

    public void closeCall( RuntimeException e, ServerCall<?, ?> call, Metadata headers, Consumer<GRpcExceptionScope.GRpcExceptionScopeBuilder> customizer) throws RuntimeException {



            Status statusToSend = Status.INTERNAL;
            Metadata metadataToSend = null;

            final Optional<HandlerMethod> handlerMethod = methodResolver.resolveMethodByThrowable(call.getMethodDescriptor().getServiceName(), e);
            if (handlerMethod.isPresent()) {
                final GRpcExceptionScope.GRpcExceptionScopeBuilder exceptionScopeBuilder = GRpcExceptionScope.builder()
                        .callHeaders(headers)
                        .methodCallAttributes(call.getAttributes())
                        .methodDescriptor(call.getMethodDescriptor())
                        .hint(GRpcRuntimeExceptionWrapper.getHint(e));
                Optional.ofNullable(customizer)
                        .ifPresent(c -> c.accept(exceptionScopeBuilder));

                final GRpcExceptionScope excScope = exceptionScopeBuilder.build();

                final HandlerMethod handler = handlerMethod.get();

                try {
                    statusToSend = handler.invoke(GRpcRuntimeExceptionWrapper.unwrap(e), excScope);
                    metadataToSend =  excScope.getResponseHeaders();
                } catch (Exception handlerException) {

                    org.slf4j.LoggerFactory.getLogger(this.getClass())
                            .error("Caught exception while executing handler method {}, returning {} status.",
                                    handler.getMethod(),
                                    statusToSend,
                                    handlerException);

                }
            }

            log.warn("Closing call with {}",statusToSend,GRpcRuntimeExceptionWrapper.unwrap(e));
            call.close(statusToSend, Optional.ofNullable(metadataToSend).orElseGet(Metadata::new));


    }




}
