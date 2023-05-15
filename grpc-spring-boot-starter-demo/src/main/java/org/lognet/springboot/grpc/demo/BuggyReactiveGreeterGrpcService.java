package org.lognet.springboot.grpc.demo;

import io.grpc.examples.reactor.ReactiveHelloRequest;
import io.grpc.examples.reactor.ReactiveHelloResponse;
import io.grpc.examples.reactor.ReactorReactiveGreeterGrpc;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;


@GRpcService
@Slf4j
@ConditionalOnClass(Transactional.class)
@Profile("reactive-buggy-security")
public class BuggyReactiveGreeterGrpcService extends ReactorReactiveGreeterGrpc.ReactiveGreeterImplBase {


    @Override
    @Secured({})
    public Mono<ReactiveHelloResponse> greet(Mono<ReactiveHelloRequest> request) {
        return super.greet(request);
    }

    @Override
    @Secured({}) //invalid
    public Mono<ReactiveHelloResponse> greet(ReactiveHelloRequest request) {
        return super.greet(request);
    }
}