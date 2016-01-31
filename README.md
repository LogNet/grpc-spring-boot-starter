#Spring boot starter for [Google RPC.](http://www.grpc.io/)
[![Build Status](https://travis-ci.org/LogNet/grpc-spring-boot-starter.svg?branch=master)](https://travis-ci.org/LogNet/grpc-spring-boot-starter)
## Features

Auto-configures and run the embedded gRPC server with discovered gRPC services. 

## Setup
TBD
<!---
```gradle
dependencies {
    compile 'org.lognet:grpc-spring-boot-starter:0.0.1'
}
```
-->
## Usage
* Start by [generating](https://github.com/google/protobuf-gradle-plugin) stub and server interface(s) from your `.proto` file(s).
* Annotate your server interface implementation(s) with `@org.lognet.springboot.grpc.GRpcService`
* Optionally configure the server port in your `application.yml/properties`. Default port is `6565`

```yaml
 grpc:
    port : 6565
```
 

## License
Apache 2.0
