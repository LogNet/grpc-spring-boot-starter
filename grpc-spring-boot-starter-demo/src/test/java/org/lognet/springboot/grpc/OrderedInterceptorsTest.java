package org.lognet.springboot.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.examples.GreeterGrpc;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.OrderedInterceptorsTest.TheConfiguration;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Created by 310242212 on 11-Sep-16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class, TheConfiguration.class},
        webEnvironment = WebEnvironment.NONE, properties = {"grpc.port=7778", "grpc.shutdownGrace=-1"})
@ActiveProfiles("disable-security")
public class OrderedInterceptorsTest extends GrpcServerTestBase {


    private static List<Integer> calledInterceptors = new ArrayList<>();


    @Before
    public void setup() {

        calledInterceptors.clear();
    }

    @Override
    protected GreeterGrpc.GreeterFutureStub beforeGreeting(GreeterGrpc.GreeterFutureStub stub) {
        Assert.assertEquals(7778, runningPort);
        Assert.assertEquals(getPort(), runningPort);
        return stub;
    }

    @Override
    protected void afterGreeting() {
        assertThat(calledInterceptors).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 10, 10, 100,100);
    }


    @TestConfiguration
    public static class TheConfiguration {
        static class OrderAwareInterceptor implements ServerInterceptor {
            private final int order;

            public OrderAwareInterceptor(int order) {
                this.order = order;
            }

            @Override
            public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                calledInterceptors.add(order);
                return next.startCall(call, headers);
            }
        }

        @Bean
        @GRpcGlobalInterceptor
        public ServerInterceptor mySixthInterceptor() {
            return new MySixthInterceptor();
        }

        class MySixthInterceptor implements ServerInterceptor, Ordered {
            @Override
            public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                calledInterceptors.add(getOrder());
                return next.startCall(call, headers);
            }

            @Override
            public int getOrder() {
                return 6;
            }
        }

        @GRpcGlobalInterceptor
        @Order(2)
        static class SecondInterceptor extends OrderAwareInterceptor {

            public SecondInterceptor() {
                super(2);
            }

        }

        @GRpcGlobalInterceptor
        @Order(4)
        static class FourthInterceptor extends OrderAwareInterceptor {

            public FourthInterceptor() {
                super(4);
            }

        }

        @GRpcGlobalInterceptor
        @Order(3)
        static class ThirdInterceptor extends OrderAwareInterceptor {

            public ThirdInterceptor() {
                super(3);
            }

        }

        @GRpcGlobalInterceptor
        @Order(1)
        static class FirstInterceptor extends OrderAwareInterceptor {

            public FirstInterceptor() {
                super(1);
            }

        }

        @GRpcGlobalInterceptor
        @Order
        // no value means lowest priority amongst all @Ordered, but higher priority than interceptors without the annotation
        static class DefaultOrderedInterceptor extends OrderAwareInterceptor {

            public DefaultOrderedInterceptor() {
                super(10);
            }

        }

        // interceptors without any annotation will always be executed last, losing to any defined @Order
        @GRpcGlobalInterceptor
        static class UnorderedInterceptor extends OrderAwareInterceptor {

            public UnorderedInterceptor() {
                super(100);
            }

        }

        @Bean
        @GRpcGlobalInterceptor
        @Order(7)
        public ServerInterceptor mySeventhInterceptor() {
            return new OrderAwareInterceptor(7);
        }

        @Bean
        @GRpcGlobalInterceptor
        @Order
        public ServerInterceptor myOrderedMethodFactoryBeanInterceptor() {
            return new OrderAwareInterceptor(10);
        }

        @Bean
        @GRpcGlobalInterceptor
        public ServerInterceptor myUnOrderedMethodFactoryBeanInterceptor() {
            return new OrderAwareInterceptor(100);
        }

        @Bean
        @GRpcGlobalInterceptor
        public ServerInterceptor myInterceptor() {
            return new MyInterceptor();
        }

        class MyInterceptor implements ServerInterceptor, Ordered {
            @Override
            public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                calledInterceptors.add(5);
                return next.startCall(call, headers);
            }

            @Override
            public int getOrder() {
                return 5;
            }
        }


        @Bean
        @Order(8)
        @GRpcGlobalInterceptor
        public ServerInterceptor my8thInterceptor() {
            return new My8Interceptor();
        }

        static class My8Interceptor implements ServerInterceptor {
            @Override
            public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                calledInterceptors.add(8);
                return next.startCall(call, headers);
            }
        }

    }
}
