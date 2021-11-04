package org.lognet.springboot.grpc.recovery;

import lombok.Getter;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Optional;

/**
 * Runtime exception that allows to wrap the checked exception to be handled by {@link GRpcExceptionHandler}
 */
public final class GRpcRuntimeExceptionWrapper extends RuntimeException {
    @Getter
    private Object hint;


    public GRpcRuntimeExceptionWrapper(@NonNull Throwable cause,Object hint){
        this(cause);
        this.hint = hint;
    }
    public GRpcRuntimeExceptionWrapper(@NonNull Throwable cause ){
        super(null,cause);
        Assert.notNull(cause,()->"Cause can't be null");
    }

    public static Throwable unwrap(Throwable exc){
        return  Optional.ofNullable(exc)
                .filter(GRpcRuntimeExceptionWrapper.class::isInstance)
                .map(Throwable::getCause)
                .orElse(exc);
    }

    public static Object getHint(Throwable exc){
        return  Optional.ofNullable(exc)
                .filter(GRpcRuntimeExceptionWrapper.class::isInstance)
                .map(GRpcRuntimeExceptionWrapper.class::cast)
                .map(w->w.getHint())
                .orElse(null);
    }

}
