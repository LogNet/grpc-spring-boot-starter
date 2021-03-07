package org.lognet.springboot.grpc.validation;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.lognet.springboot.grpc.validation.group.RequestMessage;
import org.lognet.springboot.grpc.validation.group.ResponseMessage;
import org.springframework.core.Ordered;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Optional;
import java.util.Set;


public class ValidatingInterceptor implements ServerInterceptor, Ordered {
    private Validator validator;
    @Setter
    @Accessors(fluent = true)
    private Integer order;

    public ValidatingInterceptor(Validator validator) {
        this.validator = validator;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {


        ServerCall.Listener<ReqT> listener = next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            @Override
            public void sendMessage(RespT message) {
                final Set<ConstraintViolation<RespT>> violations = validator.validate(message, ResponseMessage.class);
                if (!violations.isEmpty()) {
                    Status status = Status.FAILED_PRECONDITION.withDescription(new ConstraintViolationException(violations).getMessage());
                    delegate().close(status, headers);
                } else {
                    super.sendMessage(message);
                }

            }
        }, headers);
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
            @Override
            public void onMessage(ReqT message) {
                final Set<ConstraintViolation<ReqT>> violations = validator.validate(message, RequestMessage.class);
                if (!violations.isEmpty()) {
                    Status status = Status.INVALID_ARGUMENT.withDescription(new ConstraintViolationException(violations).getMessage());
                    call.close(status, headers);
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
