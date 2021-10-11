package org.lognet.springboot.grpc.recovery;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

public final class GRpcRuntimeExceptionWrapper extends RuntimeException {

    public GRpcRuntimeExceptionWrapper(@NonNull Throwable cause){
        super(null,cause);
        Assert.notNull(cause,()->"Cause can't be null");

    }

}
