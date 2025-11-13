package org.lognet.springboot.grpc;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.recovery.GRpcExceptionHandlerMethodResolver;
import org.lognet.springboot.grpc.recovery.GRpcExceptionScope;
import org.lognet.springboot.grpc.recovery.GRpcRuntimeExceptionWrapper;
import org.lognet.springboot.grpc.recovery.HandlerMethod;

import java.util.Optional;
import java.util.function.Consumer;

import static org.lognet.springboot.grpc.recovery.GRpcExceptionHandlerInterceptor.EXCEPTION_HANDLED;

@Slf4j
public class FailureHandlingSupport {

    private final GRpcExceptionHandlerMethodResolver methodResolver;

    public FailureHandlingSupport(GRpcExceptionHandlerMethodResolver methodResolver) {
        this.methodResolver = methodResolver;
    }

    public void closeCall(Exception e, ServerCall<?, ?> call, Metadata headers) throws RuntimeException {
        closeCall(e, call, headers, null);
    }

    public void closeCall(Exception e, ServerCall<?, ?> call, Metadata headers, Consumer<GRpcExceptionScope.GRpcExceptionScopeBuilder> customizer) throws RuntimeException {
        Optional.ofNullable(EXCEPTION_HANDLED.get()).ifPresent(h -> h.set(true));
        if (e == null) {
            log.warn("Closing null exception with {}", Status.INTERNAL);
            call.close(Status.INTERNAL, new Metadata());
        } else {
            Throwable unwrapped = GRpcRuntimeExceptionWrapper.unwrap(e);
            final Optional<HandlerMethod> handlerMethod = methodResolver.resolveMethodByThrowable(call.getMethodDescriptor().getServiceName(), unwrapped);
            if (handlerMethod.isPresent()) {
                handle(handlerMethod.get(), call, customizer, e, headers, unwrapped);
            } else if (unwrapped instanceof StatusRuntimeException || unwrapped instanceof StatusException) {
                Status status = unwrapped instanceof StatusRuntimeException ?
                        ((StatusRuntimeException) unwrapped).getStatus() :
                        ((StatusException) unwrapped).getStatus();
                Metadata metadata = unwrapped instanceof StatusRuntimeException ?
                        ((StatusRuntimeException) unwrapped).getTrailers() :
                        ((StatusException) unwrapped).getTrailers();
                log.warn("Closing call with {}", status);
                call.close(status, Optional.ofNullable(metadata).orElseGet(Metadata::new));
            } else {
                log.error("Unhandled exception", e);
                log.warn("Closing call with {}", Status.INTERNAL);
                call.close(Status.INTERNAL, new Metadata());
            }
        }
    }

    private void handle(HandlerMethod handler, ServerCall<?, ?> call, Consumer<GRpcExceptionScope.GRpcExceptionScopeBuilder> customizer, Exception e, Metadata headers, Throwable unwrapped) {
        final GRpcExceptionScope.GRpcExceptionScopeBuilder exceptionScopeBuilder = GRpcExceptionScope.builder()
                .callHeaders(headers)
                .methodCallAttributes(call.getAttributes())
                .methodDescriptor(call.getMethodDescriptor())
                .hint(GRpcRuntimeExceptionWrapper.getHint(e));

        if (customizer != null) {
            customizer.accept(exceptionScopeBuilder);
        }

        final GRpcExceptionScope excScope = exceptionScopeBuilder.build();

        try {
            Status statusToSend = handler.invoke(unwrapped, excScope);
            Metadata metadataToSend = excScope.getResponseHeaders();

            log.warn("Handled exception {} call as {}", unwrapped.getClass().getSimpleName(), statusToSend);
            call.close(statusToSend, Optional.ofNullable(metadataToSend).orElseGet(Metadata::new));
        } catch (Exception handlerException) {
            log.error("Caught exception while handling exception {} using method {}, closing with {}.",
                    unwrapped.getClass().getSimpleName(),
                    handler.getMethod(),
                    Status.INTERNAL,
                    handlerException);
            call.close(Status.INTERNAL, new Metadata());
        }
    }


}
