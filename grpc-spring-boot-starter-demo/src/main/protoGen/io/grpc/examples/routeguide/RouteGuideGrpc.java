package io.grpc.examples.routeguide;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Interface exported by the server.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.38.0)",
    comments = "Source: route_guide.proto")
public final class RouteGuideGrpc {

  private RouteGuideGrpc() {}

  public static final String SERVICE_NAME = "routeguide.RouteGuide";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.grpc.examples.routeguide.RouteNote,
      io.grpc.examples.routeguide.RouteNote> getRouteChatMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RouteChat",
      requestType = io.grpc.examples.routeguide.RouteNote.class,
      responseType = io.grpc.examples.routeguide.RouteNote.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<io.grpc.examples.routeguide.RouteNote,
      io.grpc.examples.routeguide.RouteNote> getRouteChatMethod() {
    io.grpc.MethodDescriptor<io.grpc.examples.routeguide.RouteNote, io.grpc.examples.routeguide.RouteNote> getRouteChatMethod;
    if ((getRouteChatMethod = RouteGuideGrpc.getRouteChatMethod) == null) {
      synchronized (RouteGuideGrpc.class) {
        if ((getRouteChatMethod = RouteGuideGrpc.getRouteChatMethod) == null) {
          RouteGuideGrpc.getRouteChatMethod = getRouteChatMethod =
              io.grpc.MethodDescriptor.<io.grpc.examples.routeguide.RouteNote, io.grpc.examples.routeguide.RouteNote>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RouteChat"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.routeguide.RouteNote.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.routeguide.RouteNote.getDefaultInstance()))
              .setSchemaDescriptor(new RouteGuideMethodDescriptorSupplier("RouteChat"))
              .build();
        }
      }
    }
    return getRouteChatMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RouteGuideStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RouteGuideStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RouteGuideStub>() {
        @java.lang.Override
        public RouteGuideStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RouteGuideStub(channel, callOptions);
        }
      };
    return RouteGuideStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RouteGuideBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RouteGuideBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RouteGuideBlockingStub>() {
        @java.lang.Override
        public RouteGuideBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RouteGuideBlockingStub(channel, callOptions);
        }
      };
    return RouteGuideBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RouteGuideFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RouteGuideFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RouteGuideFutureStub>() {
        @java.lang.Override
        public RouteGuideFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RouteGuideFutureStub(channel, callOptions);
        }
      };
    return RouteGuideFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Interface exported by the server.
   * </pre>
   */
  public static abstract class RouteGuideImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * A Bidirectional streaming RPC.
     * Accepts a stream of RouteNotes sent while a route is being traversed,
     * while receiving other RouteNotes (e.g. from other users).
     * </pre>
     */
    public io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.RouteNote> routeChat(
        io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.RouteNote> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getRouteChatMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getRouteChatMethod(),
            io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
              new MethodHandlers<
                io.grpc.examples.routeguide.RouteNote,
                io.grpc.examples.routeguide.RouteNote>(
                  this, METHODID_ROUTE_CHAT)))
          .build();
    }
  }

  /**
   * <pre>
   * Interface exported by the server.
   * </pre>
   */
  public static final class RouteGuideStub extends io.grpc.stub.AbstractAsyncStub<RouteGuideStub> {
    private RouteGuideStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RouteGuideStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RouteGuideStub(channel, callOptions);
    }

    /**
     * <pre>
     * A Bidirectional streaming RPC.
     * Accepts a stream of RouteNotes sent while a route is being traversed,
     * while receiving other RouteNotes (e.g. from other users).
     * </pre>
     */
    public io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.RouteNote> routeChat(
        io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.RouteNote> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getRouteChatMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   * <pre>
   * Interface exported by the server.
   * </pre>
   */
  public static final class RouteGuideBlockingStub extends io.grpc.stub.AbstractBlockingStub<RouteGuideBlockingStub> {
    private RouteGuideBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RouteGuideBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RouteGuideBlockingStub(channel, callOptions);
    }
  }

  /**
   * <pre>
   * Interface exported by the server.
   * </pre>
   */
  public static final class RouteGuideFutureStub extends io.grpc.stub.AbstractFutureStub<RouteGuideFutureStub> {
    private RouteGuideFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RouteGuideFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RouteGuideFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_ROUTE_CHAT = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final RouteGuideImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(RouteGuideImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_ROUTE_CHAT:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.routeChat(
              (io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.RouteNote>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class RouteGuideBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RouteGuideBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.grpc.examples.routeguide.RouteGuideProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RouteGuide");
    }
  }

  private static final class RouteGuideFileDescriptorSupplier
      extends RouteGuideBaseDescriptorSupplier {
    RouteGuideFileDescriptorSupplier() {}
  }

  private static final class RouteGuideMethodDescriptorSupplier
      extends RouteGuideBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    RouteGuideMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (RouteGuideGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RouteGuideFileDescriptorSupplier())
              .addMethod(getRouteChatMethod())
              .build();
        }
      }
    }
    return result;
  }
}
