package org.lognet.springboot.grpc;

import io.grpc.*;
import io.grpc.ServerCall.Listener;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterGrpc.GreeterBlockingStub;
import io.grpc.examples.GreeterOuterClass;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.OrderedInterceptorsTest.TheConfiguration;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Created by 310242212 on 11-Sep-16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class, TheConfiguration.class},
    webEnvironment = WebEnvironment.NONE, properties = "grpc.port=7778")
public class OrderedInterceptorsTest {

  private ManagedChannel channel;
  private static List<Integer> calledInterceptors = new ArrayList<>();

  @Before
  public void setup() {
    channel = ManagedChannelBuilder.forAddress("localhost", 7778)
        .usePlaintext(true)
        .build();
    calledInterceptors.clear();
  }

  @After
  public void tearDown() {
    channel.shutdown();
  }

  @Test
  public void test_interceptor_order() throws Exception {
    final GreeterBlockingStub greeterStub = GreeterGrpc.newBlockingStub(channel);
    final GreeterOuterClass.HelloRequest helloRequest = GreeterOuterClass.HelloRequest.newBuilder().setName("hello")
        .build();
    greeterStub.sayHello(helloRequest).getMessage();
    assertThat(calledInterceptors).containsExactly(1, 2, 3, 4, 10, 100);
  }

  @Configuration
  public static class TheConfiguration {

    @GRpcGlobalInterceptor
    @Order(2)
    static class SecondInterceptor implements ServerInterceptor {

      @Override
      public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
          ServerCallHandler<ReqT, RespT> next) {
        calledInterceptors.add(2);
        return next.startCall(call, headers);
      }
    }

    @GRpcGlobalInterceptor
    @Order(4)
    static class FourthInterceptor implements ServerInterceptor {

      @Override
      public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
          ServerCallHandler<ReqT, RespT> next) {
        calledInterceptors.add(4);
        return next.startCall(call, headers);
      }
    }

    @GRpcGlobalInterceptor
    @Order(3)
    static class ThirdInterceptor implements ServerInterceptor {

      @Override
      public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
          ServerCallHandler<ReqT, RespT> next) {
        calledInterceptors.add(3);
        return next.startCall(call, headers);
      }
    }

    @GRpcGlobalInterceptor
    @Order(1)
    static class FirstInterceptor implements ServerInterceptor {

      @Override
      public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
          ServerCallHandler<ReqT, RespT> next) {
        calledInterceptors.add(1);
        return next.startCall(call, headers);
      }
    }

    @GRpcGlobalInterceptor
    @Order // no value means lowest priority amongst all @Ordered, but higher priority than iceptors without the annot
    static class DefaultOrderedInterceptor implements ServerInterceptor {

      @Override
      public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                        ServerCallHandler<ReqT, RespT> next) {
        calledInterceptors.add(10);
        return next.startCall(call, headers);
      }
    }

    // interceptors without any annotation will always be executed last, losing to any defined @Order
    @GRpcGlobalInterceptor
    static class UnorderedInterceptor implements ServerInterceptor {

      @Override
      public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
          ServerCallHandler<ReqT, RespT> next) {
        calledInterceptors.add(100);
        return next.startCall(call, headers);
      }
    }
  }
}
