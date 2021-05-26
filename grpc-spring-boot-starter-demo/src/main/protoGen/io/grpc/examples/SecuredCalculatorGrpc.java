package io.grpc.examples;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.38.0)",
    comments = "Source: calculator.proto")
public final class SecuredCalculatorGrpc {

  private SecuredCalculatorGrpc() {}

  public static final String SERVICE_NAME = "SecuredCalculator";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.grpc.examples.CalculatorOuterClass.CalculatorRequest,
      io.grpc.examples.CalculatorOuterClass.CalculatorResponse> getCalculateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Calculate",
      requestType = io.grpc.examples.CalculatorOuterClass.CalculatorRequest.class,
      responseType = io.grpc.examples.CalculatorOuterClass.CalculatorResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.grpc.examples.CalculatorOuterClass.CalculatorRequest,
      io.grpc.examples.CalculatorOuterClass.CalculatorResponse> getCalculateMethod() {
    io.grpc.MethodDescriptor<io.grpc.examples.CalculatorOuterClass.CalculatorRequest, io.grpc.examples.CalculatorOuterClass.CalculatorResponse> getCalculateMethod;
    if ((getCalculateMethod = SecuredCalculatorGrpc.getCalculateMethod) == null) {
      synchronized (SecuredCalculatorGrpc.class) {
        if ((getCalculateMethod = SecuredCalculatorGrpc.getCalculateMethod) == null) {
          SecuredCalculatorGrpc.getCalculateMethod = getCalculateMethod =
              io.grpc.MethodDescriptor.<io.grpc.examples.CalculatorOuterClass.CalculatorRequest, io.grpc.examples.CalculatorOuterClass.CalculatorResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Calculate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.CalculatorOuterClass.CalculatorRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.CalculatorOuterClass.CalculatorResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SecuredCalculatorMethodDescriptorSupplier("Calculate"))
              .build();
        }
      }
    }
    return getCalculateMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SecuredCalculatorStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SecuredCalculatorStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SecuredCalculatorStub>() {
        @java.lang.Override
        public SecuredCalculatorStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SecuredCalculatorStub(channel, callOptions);
        }
      };
    return SecuredCalculatorStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SecuredCalculatorBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SecuredCalculatorBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SecuredCalculatorBlockingStub>() {
        @java.lang.Override
        public SecuredCalculatorBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SecuredCalculatorBlockingStub(channel, callOptions);
        }
      };
    return SecuredCalculatorBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SecuredCalculatorFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SecuredCalculatorFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SecuredCalculatorFutureStub>() {
        @java.lang.Override
        public SecuredCalculatorFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SecuredCalculatorFutureStub(channel, callOptions);
        }
      };
    return SecuredCalculatorFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class SecuredCalculatorImplBase implements io.grpc.BindableService {

    /**
     */
    public void calculate(io.grpc.examples.CalculatorOuterClass.CalculatorRequest request,
        io.grpc.stub.StreamObserver<io.grpc.examples.CalculatorOuterClass.CalculatorResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCalculateMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getCalculateMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.grpc.examples.CalculatorOuterClass.CalculatorRequest,
                io.grpc.examples.CalculatorOuterClass.CalculatorResponse>(
                  this, METHODID_CALCULATE)))
          .build();
    }
  }

  /**
   */
  public static final class SecuredCalculatorStub extends io.grpc.stub.AbstractAsyncStub<SecuredCalculatorStub> {
    private SecuredCalculatorStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SecuredCalculatorStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SecuredCalculatorStub(channel, callOptions);
    }

    /**
     */
    public void calculate(io.grpc.examples.CalculatorOuterClass.CalculatorRequest request,
        io.grpc.stub.StreamObserver<io.grpc.examples.CalculatorOuterClass.CalculatorResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCalculateMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class SecuredCalculatorBlockingStub extends io.grpc.stub.AbstractBlockingStub<SecuredCalculatorBlockingStub> {
    private SecuredCalculatorBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SecuredCalculatorBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SecuredCalculatorBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.examples.CalculatorOuterClass.CalculatorResponse calculate(io.grpc.examples.CalculatorOuterClass.CalculatorRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCalculateMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class SecuredCalculatorFutureStub extends io.grpc.stub.AbstractFutureStub<SecuredCalculatorFutureStub> {
    private SecuredCalculatorFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SecuredCalculatorFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SecuredCalculatorFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.grpc.examples.CalculatorOuterClass.CalculatorResponse> calculate(
        io.grpc.examples.CalculatorOuterClass.CalculatorRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCalculateMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CALCULATE = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final SecuredCalculatorImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(SecuredCalculatorImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CALCULATE:
          serviceImpl.calculate((io.grpc.examples.CalculatorOuterClass.CalculatorRequest) request,
              (io.grpc.stub.StreamObserver<io.grpc.examples.CalculatorOuterClass.CalculatorResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class SecuredCalculatorBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SecuredCalculatorBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.grpc.examples.CalculatorOuterClass.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("SecuredCalculator");
    }
  }

  private static final class SecuredCalculatorFileDescriptorSupplier
      extends SecuredCalculatorBaseDescriptorSupplier {
    SecuredCalculatorFileDescriptorSupplier() {}
  }

  private static final class SecuredCalculatorMethodDescriptorSupplier
      extends SecuredCalculatorBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    SecuredCalculatorMethodDescriptorSupplier(String methodName) {
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
      synchronized (SecuredCalculatorGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SecuredCalculatorFileDescriptorSupplier())
              .addMethod(getCalculateMethod())
              .build();
        }
      }
    }
    return result;
  }
}
