package io.grpc.examples;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.36.0)",
    comments = "Source: request_scoped.proto")
public final class RequestScopedGrpc {

  private RequestScopedGrpc() {}

  public static final String SERVICE_NAME = "RequestScoped";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage,
      io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage> getRequestScopedMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RequestScoped",
      requestType = io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage.class,
      responseType = io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage,
      io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage> getRequestScopedMethod() {
    io.grpc.MethodDescriptor<io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage, io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage> getRequestScopedMethod;
    if ((getRequestScopedMethod = RequestScopedGrpc.getRequestScopedMethod) == null) {
      synchronized (RequestScopedGrpc.class) {
        if ((getRequestScopedMethod = RequestScopedGrpc.getRequestScopedMethod) == null) {
          RequestScopedGrpc.getRequestScopedMethod = getRequestScopedMethod =
              io.grpc.MethodDescriptor.<io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage, io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RequestScoped"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage.getDefaultInstance()))
              .setSchemaDescriptor(new RequestScopedMethodDescriptorSupplier("RequestScoped"))
              .build();
        }
      }
    }
    return getRequestScopedMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RequestScopedStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RequestScopedStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RequestScopedStub>() {
        @java.lang.Override
        public RequestScopedStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RequestScopedStub(channel, callOptions);
        }
      };
    return RequestScopedStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RequestScopedBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RequestScopedBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RequestScopedBlockingStub>() {
        @java.lang.Override
        public RequestScopedBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RequestScopedBlockingStub(channel, callOptions);
        }
      };
    return RequestScopedBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RequestScopedFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RequestScopedFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RequestScopedFutureStub>() {
        @java.lang.Override
        public RequestScopedFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RequestScopedFutureStub(channel, callOptions);
        }
      };
    return RequestScopedFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class RequestScopedImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage> requestScoped(
        io.grpc.stub.StreamObserver<io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getRequestScopedMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getRequestScopedMethod(),
            io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
              new MethodHandlers<
                io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage,
                io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage>(
                  this, METHODID_REQUEST_SCOPED)))
          .build();
    }
  }

  /**
   */
  public static final class RequestScopedStub extends io.grpc.stub.AbstractAsyncStub<RequestScopedStub> {
    private RequestScopedStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RequestScopedStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RequestScopedStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage> requestScoped(
        io.grpc.stub.StreamObserver<io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getRequestScopedMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   */
  public static final class RequestScopedBlockingStub extends io.grpc.stub.AbstractBlockingStub<RequestScopedBlockingStub> {
    private RequestScopedBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RequestScopedBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RequestScopedBlockingStub(channel, callOptions);
    }
  }

  /**
   */
  public static final class RequestScopedFutureStub extends io.grpc.stub.AbstractFutureStub<RequestScopedFutureStub> {
    private RequestScopedFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RequestScopedFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RequestScopedFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_REQUEST_SCOPED = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final RequestScopedImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(RequestScopedImplBase serviceImpl, int methodId) {
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
        case METHODID_REQUEST_SCOPED:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.requestScoped(
              (io.grpc.stub.StreamObserver<io.grpc.examples.RequestScopedOuterClass.RequestScopedMessage>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class RequestScopedBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RequestScopedBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.grpc.examples.RequestScopedOuterClass.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RequestScoped");
    }
  }

  private static final class RequestScopedFileDescriptorSupplier
      extends RequestScopedBaseDescriptorSupplier {
    RequestScopedFileDescriptorSupplier() {}
  }

  private static final class RequestScopedMethodDescriptorSupplier
      extends RequestScopedBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    RequestScopedMethodDescriptorSupplier(String methodName) {
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
      synchronized (RequestScopedGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RequestScopedFileDescriptorSupplier())
              .addMethod(getRequestScopedMethod())
              .build();
        }
      }
    }
    return result;
  }
}
