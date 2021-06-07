call gradlew -S -x test   grpc-spring-boot-starter-native-demo:clean grpc-spring-boot-starter-native-demo:bootJar
call docker run --rm ^
-v "%cd%/grpc-spring-boot-starter-native-demo/build/libs:/tmp" ^
findepi/graalvm:java11-native ^
java ^
-agentlib:native-image-agent=config-output-dir=/tmp/ ^
-jar /tmp/grpc-spring-boot-starter-native-demo-4.5.2-SNAPSHOT.jar ^
--auto-stop=true ^
--spring.cloud.consul.discovery.enabled=false ^
--spring.cloud.consul.enabled=false ^
--spring.cloud.service-registry.auto-registration.enabled=false

