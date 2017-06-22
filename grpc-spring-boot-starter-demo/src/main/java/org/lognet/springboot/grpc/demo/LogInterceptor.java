package org.lognet.springboot.grpc.demo;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Created by jamessmith on 9/7/16.
 */
@Primary // set to @Primary for testing only
@Slf4j
@Component
public class LogInterceptor implements ServerInterceptor {

    private String loggerName;

    public LogInterceptor(String loggerName) {
        this.loggerName = loggerName;
    }

    public LogInterceptor() {
        this("staticServiceLogger");
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        System.out.println(loggerName + ": " + call.getMethodDescriptor().getFullMethodName());
        log.info(loggerName + ": " + call.getMethodDescriptor().getFullMethodName());
        return next.startCall(call, headers);
    }
}
