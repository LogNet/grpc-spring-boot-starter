package org.lognet.springboot.grpc;

import io.grpc.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.*;


/**
 * Created by 310242212 on 11-Sep-16.
 */
@Configuration
public class TestConfig {


    public  static final String CUSTOM_EXECUTOR_MESSAGE="Hello from custom executor.";

    @Bean(name = "globalInterceptor")
    @GRpcGlobalInterceptor
    public ServerInterceptor globalInterceptor(){

        ServerInterceptor mock = mock(ServerInterceptor.class);
        when(mock.interceptCall(notNull(ServerCall.class),notNull(Metadata.class),notNull(ServerCallHandler.class))).thenAnswer(new Answer<ServerCall.Listener>() {
            @Override
            public ServerCall.Listener answer(InvocationOnMock invocation) throws Throwable {

                return ServerCallHandler.class.cast(invocation.getArguments()[2]).startCall(
                        ServerCall.class.cast(invocation.getArguments()[0]),
                        Metadata.class.cast(invocation.getArguments()[1])
                );
            }
        });
        return mock;
    }

    @Bean
    @Profile("customServerBuilder")
    public GRpcServerBuilderConfigurer customGrpcServerBuilderConfigurer(){
        return  new GRpcServerBuilderConfigurer(){
            @Override
            public void configure(ServerBuilder<?> serverBuilder){
                 serverBuilder.executor(command -> {
                            System.out.println(CUSTOM_EXECUTOR_MESSAGE);
                            command.run();
                        }
                );

            }
        };
    }






}
