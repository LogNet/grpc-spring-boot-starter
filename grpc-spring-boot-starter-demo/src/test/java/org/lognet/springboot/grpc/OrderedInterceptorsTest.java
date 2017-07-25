package org.lognet.springboot.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterGrpc.GreeterBlockingStub;
import io.grpc.examples.GreeterOuterClass;
import java.util.ArrayList;
import java.util.List;
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


/**
 * Created by 310242212 on 11-Sep-16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class, TheConfiguration.class},
    webEnvironment = WebEnvironment.NONE, properties = "grpc.port=7777")
public class OrderedInterceptorsTest {

  private ManagedChannel channel;
  private static List<Integer> calledInterceptors = new ArrayList<>();

  @Before
  public void setup() {
    channel = ManagedChannelBuilder.forAddress("localhost", 7777)
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
    assertThat(calledInterceptors).containsExactly(4, 3, 2, 1);
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
  }
}
