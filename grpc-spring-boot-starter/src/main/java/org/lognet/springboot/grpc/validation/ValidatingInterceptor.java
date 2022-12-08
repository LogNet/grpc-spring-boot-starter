package org.lognet.springboot.grpc.validation;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.lognet.springboot.grpc.FailureHandlingSupport;
import org.lognet.springboot.grpc.MessageBlockingServerCallListener;
import org.lognet.springboot.grpc.recovery.GRpcRuntimeExceptionWrapper;
import org.lognet.springboot.grpc.validation.group.RequestMessage;
import org.lognet.springboot.grpc.validation.group.ResponseMessage;
import org.springframework.core.Ordered;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;


public class ValidatingInterceptor implements ServerInterceptor, Ordered {
    private final Validator validator;

    @Setter
    @Accessors(fluent = true)
    private Integer order;

    private final FailureHandlingSupport failureHandlingSupport;


    public ValidatingInterceptor(Validator validator,FailureHandlingSupport failureHandlingSupport) {
        this.validator = validator;
        this.failureHandlingSupport = failureHandlingSupport;
    }


    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {


        final ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> validationServerCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {

            @Override
            public void sendMessage(RespT message) {
                final Set<ConstraintViolation<RespT>> violations = validator.validate(message, ResponseMessage.class);
                if (!violations.isEmpty()) {
                    GRpcRuntimeExceptionWrapper exception = new GRpcRuntimeExceptionWrapper(new ConstraintViolationException(violations), Status.FAILED_PRECONDITION);
                    failureHandlingSupport.closeCall(exception, this , headers, b -> b.response(message));
                } else {
                    super.sendMessage(message);
                }
            }
        };
        ServerCall.Listener<ReqT> listener = next.startCall(validationServerCall, headers);

        return new MessageBlockingServerCallListener<ReqT>(listener) {

            @Override
            public void onMessage(ReqT message) {
                final Set<ConstraintViolation<ReqT>> violations = validator.validate(message, RequestMessage.class);
                if (!violations.isEmpty()) {
                    blockMessage();
                    final GRpcRuntimeExceptionWrapper exception = new GRpcRuntimeExceptionWrapper(new ConstraintViolationException(violations), Status.INVALID_ARGUMENT);
                    failureHandlingSupport.closeCall(exception,  validationServerCall, headers, b -> b.request(message));
                } else {
                    super.onMessage(message);
                }

            }
        };

    }

    @Override
    public int getOrder() {
        return Optional.ofNullable(order).orElse(HIGHEST_PRECEDENCE + 10);
    }
}
