#Spring boot starter for [gRPC framework.](http://www.grpc.io/)
[![Build Status](https://travis-ci.org/LogNet/grpc-spring-boot-starter.svg?branch=master)](https://travis-ci.org/LogNet/grpc-spring-boot-starter)
[ ![Download](https://api.bintray.com/packages/lognet/maven/grpc-spring-boot-starter/images/download.svg) ](https://bintray.com/lognet/maven/grpc-spring-boot-starter/_latestVersion)
## Features

Auto-configures and run the embedded gRPC server with @GRpcService-enabled beans as part of spring-boot application.

## Setup

```gradle
repositories {  
   jcenter()  
   // maven { url "http://oss.jfrog.org/oss-snapshot-local" } //for snashot builds
   
}
dependencies {
    compile('org.lognet:grpc-spring-boot-starter:0.0.4')
}
```
If you are using protobuf version lower than `3.0.0`, please use `org.lognet:grpc-spring-boot-starter:0.0.3` 


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
Note the generated `io.grpc.examples.GreeterGrpc.GreeterImplBase` class that extends `io.grpc.BindableService`.(The generated classes were intentionally  committed for demo purposes).

All you need to do is to annotate your service implementation with `@org.lognet.springboot.grpc.GRpcService`

```java
@GRpcService
    public static class GreeterService extends  GreeterGrpc.GreeterImplBase{
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
