package org.lognet.springboot.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import static   org.mockito.Mockito.*;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



/**
 * Created by 310242212 on 11-Sep-16.
 */
@Configuration
public class TestConfig {



    @Bean(name = "globalInterceptor")
    @GRpcGlobalInterceptor
    public ServerInterceptor globalInterceptor(){

        ServerInterceptor mock = mock(ServerInterceptor.class);
        when(mock.interceptCall(notNull(ServerCall.class),notNull(Metadata.class),notNull(ServerCallHandler.class))).thenAnswer(new Answer<ServerCall.Listener>() {
            @Override
            public ServerCall.Listener answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgumentAt(2,ServerCallHandler.class).startCall(invocation.getArgumentAt(0,ServerCall.class),
                        invocation.getArgumentAt(1,Metadata.class)
                );
            }
        });
        return mock;
    }






}
