#Spring boot starter for [Google RPC.](http://www.grpc.io/)
[![Build Status](https://travis-ci.org/LogNet/grpc-spring-boot-starter.svg?branch=master)](https://travis-ci.org/LogNet/grpc-spring-boot-starter)
[ ![Download](https://api.bintray.com/packages/lognet/maven/grpc-spring-boot-starter/images/download.svg) ](https://bintray.com/lognet/maven/grpc-spring-boot-starter/_latestVersion)
## Features

Auto-configures and run the embedded gRPC server with @GRpcService-enabled beans as part of spring-boot application.

## Setup

```gradle
repositories {  
   jcenter()  
}
dependencies {
    compile('org.lognet:grpc-spring-boot-starter:0.0.2')
}
```

## Usage
* Start by [generating](https://github.com/google/protobuf-gradle-plugin) stub and server interface(s) from your `.proto` file(s).
* Annotate your server interface implementation(s) with `@org.lognet.springboot.grpc.GRpcService`
* Optionally configure the server port in your `application.yml/properties`. Default port is `6565`

```yaml
 grpc:
    port : 6565
```
## Show case
In the 'grpc-spring-boot-starter-demo' project you can find fully functional example with integration test.
The service definition from `.proto` file looks like this :
```proto
service Greeter {
    rpc SayHello ( HelloRequest) returns (  HelloReply) {}
}
```
Note the generated `io.grpc.examples.GreeterGrpc` class with static function `bindService`.(The generated classes were intentionally  committed for demo purposes).

All you need to do is to annotate your service implementation with `@org.lognet.springboot.grpc.GRpcService`

```java
@GRpcService(grpcServiceOuterClass = GreeterGrpc.class)
    public static class GreeterService implements GreeterGrpc.Greeter{
        @Override
        public void sayHello(GreeterOuterClass.HelloRequest request, StreamObserver<GreeterOuterClass.HelloReply> responseObserver) {
            final GreeterOuterClass.HelloReply.Builder replyBuilder = GreeterOuterClass.HelloReply.newBuilder().setMessage("Hello " + request.getName());
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }
    }
```

## On the roadmap
* Customized gRPC server builder with compression/decompression registry, custom `Executor` service   and transport security.
* `ServerInterceptor` support.


## License
Apache 2.0
