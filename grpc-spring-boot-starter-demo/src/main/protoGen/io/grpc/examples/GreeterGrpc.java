package io.grpc.examples;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * The greeter service definition.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.38.0)",
    comments = "Source: greeter.proto")
public final class GreeterGrpc {

  private GreeterGrpc() {}

  public static final String SERVICE_NAME = "Greeter";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.grpc.examples.GreeterOuterClass.HelloRequest,
      io.grpc.examples.GreeterOuterClass.HelloReply> getSayHelloMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SayHello",
      requestType = io.grpc.examples.GreeterOuterClass.HelloRequest.class,
      responseType = io.grpc.examples.GreeterOuterClass.HelloReply.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.grpc.examples.GreeterOuterClass.HelloRequest,
      io.grpc.examples.GreeterOuterClass.HelloReply> getSayHelloMethod() {
    io.grpc.MethodDescriptor<io.grpc.examples.GreeterOuterClass.HelloRequest, io.grpc.examples.GreeterOuterClass.HelloReply> getSayHelloMethod;
    if ((getSayHelloMethod = GreeterGrpc.getSayHelloMethod) == null) {
      synchronized (GreeterGrpc.class) {
        if ((getSayHelloMethod = GreeterGrpc.getSayHelloMethod) == null) {
          GreeterGrpc.getSayHelloMethod = getSayHelloMethod =
              io.grpc.MethodDescriptor.<io.grpc.examples.GreeterOuterClass.HelloRequest, io.grpc.examples.GreeterOuterClass.HelloReply>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SayHello"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.GreeterOuterClass.HelloRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.GreeterOuterClass.HelloReply.getDefaultInstance()))
              .setSchemaDescriptor(new GreeterMethodDescriptorSupplier("SayHello"))
              .build();
        }
      }
    }
    return getSayHelloMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      io.grpc.examples.GreeterOuterClass.HelloReply> getSayAuthHelloMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SayAuthHello",
      requestType = com.google.protobuf.Empty.class,
      responseType = io.grpc.examples.GreeterOuterClass.HelloReply.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      io.grpc.examples.GreeterOuterClass.HelloReply> getSayAuthHelloMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, io.grpc.examples.GreeterOuterClass.HelloReply> getSayAuthHelloMethod;
    if ((getSayAuthHelloMethod = GreeterGrpc.getSayAuthHelloMethod) == null) {
      synchronized (GreeterGrpc.class) {
        if ((getSayAuthHelloMethod = GreeterGrpc.getSayAuthHelloMethod) == null) {
          GreeterGrpc.getSayAuthHelloMethod = getSayAuthHelloMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, io.grpc.examples.GreeterOuterClass.HelloReply>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SayAuthHello"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.GreeterOuterClass.HelloReply.getDefaultInstance()))
              .setSchemaDescriptor(new GreeterMethodDescriptorSupplier("SayAuthHello"))
              .build();
        }
      }
    }
    return getSayAuthHelloMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      io.grpc.examples.GreeterOuterClass.HelloReply> getSayAuthOnlyHelloMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SayAuthOnlyHello",
      requestType = com.google.protobuf.Empty.class,
      responseType = io.grpc.examples.GreeterOuterClass.HelloReply.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      io.grpc.examples.GreeterOuterClass.HelloReply> getSayAuthOnlyHelloMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, io.grpc.examples.GreeterOuterClass.HelloReply> getSayAuthOnlyHelloMethod;
    if ((getSayAuthOnlyHelloMethod = GreeterGrpc.getSayAuthOnlyHelloMethod) == null) {
      synchronized (GreeterGrpc.class) {
        if ((getSayAuthOnlyHelloMethod = GreeterGrpc.getSayAuthOnlyHelloMethod) == null) {
          GreeterGrpc.getSayAuthOnlyHelloMethod = getSayAuthOnlyHelloMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, io.grpc.examples.GreeterOuterClass.HelloReply>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SayAuthOnlyHello"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.GreeterOuterClass.HelloReply.getDefaultInstance()))
              .setSchemaDescriptor(new GreeterMethodDescriptorSupplier("SayAuthOnlyHello"))
              .build();
        }
      }
    }
    return getSayAuthOnlyHelloMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.grpc.examples.GreeterOuterClass.Person,
      io.grpc.examples.GreeterOuterClass.Person> getHelloPersonValidResponseMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HelloPersonValidResponse",
      requestType = io.grpc.examples.GreeterOuterClass.Person.class,
      responseType = io.grpc.examples.GreeterOuterClass.Person.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.grpc.examples.GreeterOuterClass.Person,
      io.grpc.examples.GreeterOuterClass.Person> getHelloPersonValidResponseMethod() {
    io.grpc.MethodDescriptor<io.grpc.examples.GreeterOuterClass.Person, io.grpc.examples.GreeterOuterClass.Person> getHelloPersonValidResponseMethod;
    if ((getHelloPersonValidResponseMethod = GreeterGrpc.getHelloPersonValidResponseMethod) == null) {
      synchronized (GreeterGrpc.class) {
        if ((getHelloPersonValidResponseMethod = GreeterGrpc.getHelloPersonValidResponseMethod) == null) {
          GreeterGrpc.getHelloPersonValidResponseMethod = getHelloPersonValidResponseMethod =
              io.grpc.MethodDescriptor.<io.grpc.examples.GreeterOuterClass.Person, io.grpc.examples.GreeterOuterClass.Person>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "HelloPersonValidResponse"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.GreeterOuterClass.Person.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.GreeterOuterClass.Person.getDefaultInstance()))
              .setSchemaDescriptor(new GreeterMethodDescriptorSupplier("HelloPersonValidResponse"))
              .build();
        }
      }
    }
    return getHelloPersonValidResponseMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.grpc.examples.GreeterOuterClass.Person,
      io.grpc.examples.GreeterOuterClass.Person> getHelloPersonInvalidResponseMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HelloPersonInvalidResponse",
      requestType = io.grpc.examples.GreeterOuterClass.Person.class,
      responseType = io.grpc.examples.GreeterOuterClass.Person.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.grpc.examples.GreeterOuterClass.Person,
      io.grpc.examples.GreeterOuterClass.Person> getHelloPersonInvalidResponseMethod() {
    io.grpc.MethodDescriptor<io.grpc.examples.GreeterOuterClass.Person, io.grpc.examples.GreeterOuterClass.Person> getHelloPersonInvalidResponseMethod;
    if ((getHelloPersonInvalidResponseMethod = GreeterGrpc.getHelloPersonInvalidResponseMethod) == null) {
      synchronized (GreeterGrpc.class) {
        if ((getHelloPersonInvalidResponseMethod = GreeterGrpc.getHelloPersonInvalidResponseMethod) == null) {
          GreeterGrpc.getHelloPersonInvalidResponseMethod = getHelloPersonInvalidResponseMethod =
              io.grpc.MethodDescriptor.<io.grpc.examples.GreeterOuterClass.Person, io.grpc.examples.GreeterOuterClass.Person>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "HelloPersonInvalidResponse"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.GreeterOuterClass.Person.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.GreeterOuterClass.Person.getDefaultInstance()))
              .setSchemaDescriptor(new GreeterMethodDescriptorSupplier("HelloPersonInvalidResponse"))
              .build();
        }
      }
    }
    return getHelloPersonInvalidResponseMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static GreeterStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GreeterStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GreeterStub>() {
        @java.lang.Override
        public GreeterStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GreeterStub(channel, callOptions);
        }
      };
    return GreeterStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static GreeterBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GreeterBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GreeterBlockingStub>() {
        @java.lang.Override
        public GreeterBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GreeterBlockingStub(channel, callOptions);
        }
      };
    return GreeterBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static GreeterFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GreeterFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GreeterFutureStub>() {
        @java.lang.Override
        public GreeterFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GreeterFutureStub(channel, callOptions);
        }
      };
    return GreeterFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * The greeter service definition.
   * </pre>
   */
  public static abstract class GreeterImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Sends a greeting
     * </pre>
     */
    public void sayHello(io.grpc.examples.GreeterOuterClass.HelloRequest request,
        io.grpc.stub.StreamObserver<io.grpc.examples.GreeterOuterClass.HelloReply> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSayHelloMethod(), responseObserver);
    }

    /**
     */
    public void sayAuthHello(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<io.grpc.examples.GreeterOuterClass.HelloReply> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSayAuthHelloMethod(), responseObserver);
    }

    /**
     */
    public void sayAuthOnlyHello(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<io.grpc.examples.GreeterOuterClass.HelloReply> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSayAuthOnlyHelloMethod(), responseObserver);
    }

    /**
     */
    public void helloPersonValidResponse(io.grpc.examples.GreeterOuterClass.Person request,
        io.grpc.stub.StreamObserver<io.grpc.examples.GreeterOuterClass.Person> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHelloPersonValidResponseMethod(), responseObserver);
    }

    /**
     */
    public void helloPersonInvalidResponse(io.grpc.examples.GreeterOuterClass.Person request,
        io.grpc.stub.StreamObserver<io.grpc.examples.GreeterOuterClass.Person> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHelloPersonInvalidResponseMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSayHelloMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.grpc.examples.GreeterOuterClass.HelloRequest,
                io.grpc.examples.GreeterOuterClass.HelloReply>(
                  this, METHODID_SAY_HELLO)))
          .addMethod(
            getSayAuthHelloMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                io.grpc.examples.GreeterOuterClass.HelloReply>(
                  this, METHODID_SAY_AUTH_HELLO)))
          .addMethod(
            getSayAuthOnlyHelloMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                io.grpc.examples.GreeterOuterClass.HelloReply>(
                  this, METHODID_SAY_AUTH_ONLY_HELLO)))
          .addMethod(
            getHelloPersonValidResponseMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.grpc.examples.GreeterOuterClass.Person,
                io.grpc.examples.GreeterOuterClass.Person>(
                  this, METHODID_HELLO_PERSON_VALID_RESPONSE)))
          .addMethod(
            getHelloPersonInvalidResponseMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.grpc.examples.GreeterOuterClass.Person,
                io.grpc.examples.GreeterOuterClass.Person>(
                  this, METHODID_HELLO_PERSON_INVALID_RESPONSE)))
          .build();
    }
  }

  /**
   * <pre>
   * The greeter service definition.
   * </pre>
   */
  public static final class GreeterStub extends io.grpc.stub.AbstractAsyncStub<GreeterStub> {
    private GreeterStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GreeterStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GreeterStub(channel, callOptions);
    }

    /**
     * <pre>
     * Sends a greeting
     * </pre>
     */
    public void sayHello(io.grpc.examples.GreeterOuterClass.HelloRequest request,
        io.grpc.stub.StreamObserver<io.grpc.examples.GreeterOuterClass.HelloReply> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSayHelloMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sayAuthHello(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<io.grpc.examples.GreeterOuterClass.HelloReply> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSayAuthHelloMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sayAuthOnlyHello(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<io.grpc.examples.GreeterOuterClass.HelloReply> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSayAuthOnlyHelloMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void helloPersonValidResponse(io.grpc.examples.GreeterOuterClass.Person request,
        io.grpc.stub.StreamObserver<io.grpc.examples.GreeterOuterClass.Person> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHelloPersonValidResponseMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void helloPersonInvalidResponse(io.grpc.examples.GreeterOuterClass.Person request,
        io.grpc.stub.StreamObserver<io.grpc.examples.GreeterOuterClass.Person> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHelloPersonInvalidResponseMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * The greeter service definition.
   * </pre>
   */
  public static final class GreeterBlockingStub extends io.grpc.stub.AbstractBlockingStub<GreeterBlockingStub> {
    private GreeterBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GreeterBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GreeterBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Sends a greeting
     * </pre>
     */
    public io.grpc.examples.GreeterOuterClass.HelloReply sayHello(io.grpc.examples.GreeterOuterClass.HelloRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSayHelloMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.grpc.examples.GreeterOuterClass.HelloReply sayAuthHello(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSayAuthHelloMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.grpc.examples.GreeterOuterClass.HelloReply sayAuthOnlyHello(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSayAuthOnlyHelloMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.grpc.examples.GreeterOuterClass.Person helloPersonValidResponse(io.grpc.examples.GreeterOuterClass.Person request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHelloPersonValidResponseMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.grpc.examples.GreeterOuterClass.Person helloPersonInvalidResponse(io.grpc.examples.GreeterOuterClass.Person request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHelloPersonInvalidResponseMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * The greeter service definition.
   * </pre>
   */
  public static final class GreeterFutureStub extends io.grpc.stub.AbstractFutureStub<GreeterFutureStub> {
    private GreeterFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GreeterFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GreeterFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Sends a greeting
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.grpc.examples.GreeterOuterClass.HelloReply> sayHello(
        io.grpc.examples.GreeterOuterClass.HelloRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSayHelloMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.grpc.examples.GreeterOuterClass.HelloReply> sayAuthHello(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSayAuthHelloMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.grpc.examples.GreeterOuterClass.HelloReply> sayAuthOnlyHello(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSayAuthOnlyHelloMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.grpc.examples.GreeterOuterClass.Person> helloPersonValidResponse(
        io.grpc.examples.GreeterOuterClass.Person request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHelloPersonValidResponseMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.grpc.examples.GreeterOuterClass.Person> helloPersonInvalidResponse(
        io.grpc.examples.GreeterOuterClass.Person request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHelloPersonInvalidResponseMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SAY_HELLO = 0;
  private static final int METHODID_SAY_AUTH_HELLO = 1;
  private static final int METHODID_SAY_AUTH_ONLY_HELLO = 2;
  private static final int METHODID_HELLO_PERSON_VALID_RESPONSE = 3;
  private static final int METHODID_HELLO_PERSON_INVALID_RESPONSE = 4;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final GreeterImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(GreeterImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SAY_HELLO:
          serviceImpl.sayHello((io.grpc.examples.GreeterOuterClass.HelloRequest) request,
              (io.grpc.stub.StreamObserver<io.grpc.examples.GreeterOuterClass.HelloReply>) responseObserver);
          break;
        case METHODID_SAY_AUTH_HELLO:
          serviceImpl.sayAuthHello((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<io.grpc.examples.GreeterOuterClass.HelloReply>) responseObserver);
          break;
        case METHODID_SAY_AUTH_ONLY_HELLO:
          serviceImpl.sayAuthOnlyHello((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<io.grpc.examples.GreeterOuterClass.HelloReply>) responseObserver);
          break;
        case METHODID_HELLO_PERSON_VALID_RESPONSE:
          serviceImpl.helloPersonValidResponse((io.grpc.examples.GreeterOuterClass.Person) request,
              (io.grpc.stub.StreamObserver<io.grpc.examples.GreeterOuterClass.Person>) responseObserver);
          break;
        case METHODID_HELLO_PERSON_INVALID_RESPONSE:
          serviceImpl.helloPersonInvalidResponse((io.grpc.examples.GreeterOuterClass.Person) request,
              (io.grpc.stub.StreamObserver<io.grpc.examples.GreeterOuterClass.Person>) responseObserver);
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

  private static abstract class GreeterBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    GreeterBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.grpc.examples.GreeterOuterClass.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Greeter");
    }
  }

  private static final class GreeterFileDescriptorSupplier
      extends GreeterBaseDescriptorSupplier {
    GreeterFileDescriptorSupplier() {}
  }

  private static final class GreeterMethodDescriptorSupplier
      extends GreeterBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    GreeterMethodDescriptorSupplier(String methodName) {
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
      synchronized (GreeterGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new GreeterFileDescriptorSupplier())
              .addMethod(getSayHelloMethod())
              .addMethod(getSayAuthHelloMethod())
              .addMethod(getSayAuthOnlyHelloMethod())
              .addMethod(getHelloPersonValidResponseMethod())
              .addMethod(getHelloPersonInvalidResponseMethod())
              .build();
        }
      }
    }
    return result;
  }
}
