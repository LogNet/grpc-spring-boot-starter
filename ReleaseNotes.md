| Starter Version      | gRPC versions |Spring Boot version
| -------------------- |:-------------:|:------------------:|
| [4.5.0](#version-450)| 1.37.0        |2.4.5               |
| [4.4.7](#version-447)| 1.37.0        |2.4.5               |
| [4.4.6](#version-446)| 1.37.0        |2.4.5               |
| [4.4.5](#version-445)| 1.36.0        |2.4.1               |
| [4.4.4](#version-444)| 1.35.0        |2.4.1               |
| [4.4.3](#version-443)| 1.35.0        |2.4.1               |
| [4.4.2](#version-442)| 1.34.1        |2.4.1               |
| [4.4.1](#version-443)| 1.34.1        |2.3.4.RELEASE       |
| [4.4.0](#version-440)| 1.34.1        |2.3.4.RELEASE       |
| [4.3.1](#version-431)| 1.34.1        |2.3.4.RELEASE       |
| [4.3.0](#version-430)| 1.34.1        |2.3.4.RELEASE       |
| [4.2.3](#version-423)| 1.33.1        |2.3.4.RELEASE       |
| [4.2.2](#version-422)| 1.33.0        |2.3.4.RELEASE       |
| [4.2.1](#version-421)| 1.33.0        |2.3.4.RELEASE       |
| [4.2.0](#version-420)| 1.33.0        |2.3.3.RELEASE       |
| [4.1.0](#version-410)| 1.32.2        |2.3.3.RELEASE       |
| [4.0.0](#version-400)| 1.32.1        |2.3.3.RELEASE       |
| [3.5.7](#version-357)| 1.31.1        |1.5.13.RELEASE      |




# Version 4.5.0

## :beetle: Bug Fixes

- How to disable Grpc Security [#206](https://github.com/LogNet/grpc-spring-boot-starter/issues/206)
## :hammer_and_wrench:  Migration from 4.4.x


Please use standard `@Configuration` instead of `@EnableGrpcSecurity` :

Before (4.4.x):
```java
@EnableGrpcSecurity
public class GrpcSecurityConfiguration extends GrpcSecurityConfigurerAdapter{
    
}
```
After (4.5.0) :
```java
@Configuration
public class GrpcSecurityConfiguration extends GrpcSecurityConfigurerAdapter{
    
}
```
or

```java
@Configuration
public class MyAppConfiguration {
    public class GrpcSecurityConfiguration extends GrpcSecurityConfigurerAdapter {

    }

    @Bean
    public GrpcSecurityConfigurerAdapter grpcConfig(){
        return  new GrpcSecurityConfiguration();
    }
}
```
# Version 4.4.7 

## :star: New Features

- grpc-netty dependency support [#203](https://github.com/LogNet/grpc-spring-boot-starter/issues/203)

## :beetle: Bug Fixes

- GrpcSecurityConfigurerAdapter initialization failure without spring-security-oauth2-resource-server [#176](https://github.com/LogNet/grpc-spring-boot-starter/issues/176)


# Version 4.4.6 

## :star: New Features

- Support Springs `@Ordered` Annotation on GRpcServerRunner [#126](https://github.com/LogNet/grpc-spring-boot-starter/issues/126)

## :notebook_with_decorative_cover: Documentation

- Document `@Transactional` usage [#195](https://github.com/LogNet/grpc-spring-boot-starter/issues/195)

## :hammer: Dependency Upgrades

- Bump gRPC to 1.37.0 and spring-boot to 2.4.5  [#202](https://github.com/LogNet/grpc-spring-boot-starter/issues/202)


# Version 4.4.5 

## :star: New Features

- Enable users to configure the built-in interceptor precedence [#193](https://github.com/LogNet/grpc-spring-boot-starter/issues/193)
- Allow to add custom tag to the MonitoringServerInterceptor [#191](https://github.com/LogNet/grpc-spring-boot-starter/issues/191)
- Allow to pass custom metadata during authentication failure [#189](https://github.com/LogNet/grpc-spring-boot-starter/issues/189)

## :hammer: Dependency Upgrades

- Upgrade to grpc 1.36.0 [#194](https://github.com/LogNet/grpc-spring-boot-starter/issues/194)


# Version 4.4.4 
* Fixes https://github.com/LogNet/grpc-spring-boot-starter/issues/181[181]

# Version 4.4.3 
* Fixes https://github.com/LogNet/grpc-spring-boot-starter/issues/178[178]
* Fixes https://github.com/LogNet/grpc-spring-boot-starter/issues/117[117]
* Fixes https://github.com/LogNet/grpc-spring-boot-starter/issues/180[180]
* gRPC response status set to `PERMISSION_DENIED` when user has insufficient privileges to invoke gRPC method.
* gRPC version upgraded to `1.35.0`

# Version 4.4.2 
* Spring Boot `2.4.1`
* Spring Cloud `2020.0.0`

# Version 4.4.1 
* If more than 1 port is exposed, add `address` tag to Micrometer's `timer`

# Version 4.4.0 
* gRPC server metrics support via https://micrometer.io[micrometer.io] integration

# Version 4.3.1 
* Fixes https://github.com/LogNet/grpc-spring-boot-starter/issues/169[169]

# Version 4.3.0 
* Spring Validation (Java Beans) support
* gRPC version upgraded to 1.34.1

# Version 4.2.3 
* gRPC version upgraded to 1.33.1
* Support `authenticate only` when role list is empty , `@Secured({})` (credits to https://github.com/CleverUnderDog[@CleverUnderDog])

# Version 4.2.2 
* Fixes https://github.com/LogNet/grpc-spring-boot-starter/issues/162[162]

# Version 4.2.1 

* Configure bind address and other netty settings (fixes https://github.com/LogNet/grpc-spring-boot-starter/issues/82[82])
* When overriding default GRPC security configuration, `@Secured` annotation is enabled by default.

#### Breaking changes

* The starter brings now `io.grpc:grpc-netty-shaded` instead of `io.grpc:grpc-netty` as transitive dependency (fixes https://github.com/LogNet/grpc-spring-boot-starter/issues/108[108]). +
This  means that  all classes from `io.grpc.netty` package should be imported from `io.grpc.netty.shaded.io.grpc.netty`

# Version 4.2.0 
* gRPC version upgraded to 1.33.0
* Fixed the issue with default method-level `@Secured` annotation (see #159)

# Version 4.1.0 
* gRPC version upgraded to 1.32.2
* Fixed the issue with  obtaining `Authentication` details in secured method implementation.
* Fixed the issue with providing client-side user credentials.

# Version 4.0.0 
* Spring Security framework integration
* gRPC version upgraded to 1.32.1
* Spring Boot 2.3.3

[IMPORTANT]
Please use `4.1.0` version, `4.0.0` has issue with obtaining Authentication details in secured object implementation.

# Version 3.5.7 
* gRPC version upgraded to 1.31.1

# Version 3.5.6 
* gRPC version upgraded to 1.30.2

# Version 3.5.5 
* gRPC version upgraded to 1.30.0
* Consul health check support for GRPC service (credits to https://github.com/evk1986[@evk1986])

# Version 3.5.4 
* gRPC version upgraded to 1.29.0

# Version 3.5.3 
* gRPC version upgraded to 1.28.0

# Version 3.5.2 
* gRPC version upgraded to 1.27.2
* Gradle 5.6.3

# Version 3.5.1 
* Graceful shutdown timeout setting
* Fixed potential race condition issue when defining running port.


# Version 3.5.0 
* gRPC version upgraded to 1.25.0

# Version 3.4.3 
* Spring boot 1.2.x compatibility

# Version 3.4.2 
* gRPC version upgraded to 1.24.0

# Version 3.4.1 
* gRPC version upgraded to 1.23.0

# Version 3.4.0 
* gRPC version upgraded to 1.22.1
* TLS support

# Version 3.3.0 
* gRPC version upgraded to 1.21.0
* Consul auto-registration support

# Version 3.2.2 
* gRPC version upgraded to 1.20.0

# Version 3.2.1 
* Closes #103 and #99
Property `local.grpc.port` was removed , please use `@LocalRunningGrpcPort` annotation on `int` field to get running port.

# Version 3.2.0 
* gRPC version upgraded to 1.19.0

# Version 3.1.0 
* gRPC version upgraded to 1.18.0

# Version 3.0.2 
* gRPC version upgraded to 1.17.1

# Version 3.0.1 
* gRPC version upgraded to 1.16.1

# Version 3.0.0 
* The artifacts are published to *maven central*.
  Pay attention that group has changed from `org.lognet` to `io.github.lognet`.

# Version 2.4.3 
* gRPC version upgraded to 1.15.1

# Version 2.4.2 
* gRPC version upgraded to 1.15.0

# Version 2.4.1 
* Gradle 4.10
* Fixes #93

# Version 2.4.0 
* gRPC version upgraded to 1.13.1
* Tested with
    ** springBoot_1_X_Version = '1.5.13.RELEASE'
    ** springBoot_2_X_Version = '2.0.3.RELEASE'

# Version 2.3.2 
* Server reflection support

# Version 2.3.1 
* Closes #73

# Version 2.3.0 
* gRPC version upgraded to 1.11.0
* Fixed #80
* Added Spring boot 2.X demo project
* Spring boot  upgraded to 1.5.11
* Tested with
    ** springBoot_1_X_Version = '1.5.11.RELEASE'
    ** springBoot_2_X_Version = '2.0.1.RELEASE'


# Version 2.2.0 
- gRPC version upgraded to 1.10.0
- Gradle 4.6

# Version 2.1.5 
- gRPC version upgraded to 1.9.0
- Spring boot  upgraded to 1.5.9

# Version 2.1.4 
- gRPC version upgraded to 1.8.0

# Version 2.1.3 
- Random gRPC server port support
- Fixed wrong interceptors ordering bug.
- gRPC version upgraded to 1.7.0
- Spring boot  upgraded to 1.5.8

# Version 2.1.0 
- gRPC version upgraded to 1.6.1
- Spring boot  upgraded to 1.5.6
- In process server support

# Version 2.0.5 
- HealthStatusManager exposed as Spring bean
- gRPC version upgraded to 1.5.0
- Ordered execution support of global server interceptors.

# Version 2.0.4 
- gRPC version upgraded to 1.4.0

# Version 2.0.3 
- gRPC version upgraded to 1.3.0

# Version 2.0.2 
- Fixing issue to identify beans with annotation: see PR #38
- Spring Boot version upgraded to  1.5.2

# Version 2.0.1 
- gRPC version upgraded to 1.2.0
- Spring Boot version upgraded to  1.4.5

# Version 2.0.0 
- gRPC version upgraded to 1.1.1
- Spring Boot version upgraded to  1.4.4
- *Breaking API change*: +
    `void GRpcServerBuilderConfigurer::configure(ServerBuilder<?> serverBuilder)` method now returns void and overriding of `ServerBuilder` is not supported +
     to prevent potential confusion.

