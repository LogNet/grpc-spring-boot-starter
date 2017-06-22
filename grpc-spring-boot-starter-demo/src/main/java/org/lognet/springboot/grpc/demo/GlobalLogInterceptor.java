package org.lognet.springboot.grpc.demo;

import org.lognet.springboot.grpc.GRpcGlobalInterceptor;

/**
 * Created by jamessmith on 9/7/16.
 */
@GRpcGlobalInterceptor
public class GlobalLogInterceptor extends LogInterceptor {

    public GlobalLogInterceptor() {
        super("staticGlobalLogger");
    }
}
