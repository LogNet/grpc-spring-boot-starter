package org.lognet.springboot.grpc.demo;

import io.grpc.Status;
import io.grpc.examples.reactor.ReactiveHelloRequest;
import io.grpc.examples.reactor.ReactiveHelloResponse;
import io.grpc.examples.reactor.ReactorReactiveGreeterGrpc;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.recovery.GRpcExceptionHandler;
import org.lognet.springboot.grpc.recovery.GRpcExceptionScope;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.stream.IntStream;


@GRpcService
@Slf4j
@ConditionalOnClass(Transactional.class)
public class ReactiveGreeterGrpcService extends ReactorReactiveGreeterGrpc.ReactiveGreeterImplBase {

    private ReactiveGreeterService reactiveGreeterService;

    public ReactiveGreeterGrpcService(ReactiveGreeterService reactiveGreeterService) {
        this.reactiveGreeterService = reactiveGreeterService;
    }

    @Override
    @Secured({})
    public Mono<ReactiveHelloResponse> greet(Mono<ReactiveHelloRequest> request) {
        Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .orElseThrow();
        return reactiveGreeterService.greet(request);

    }

    @Override
    public Flux<ReactiveHelloResponse> multiGreet(Mono<ReactiveHelloRequest> request) {
        return request.flatMapIterable(r ->
                IntStream.range(0, r.getName().length())
                        .mapToObj(i -> ReactiveHelloResponse.newBuilder()
                                .setMessage(String.format("Hello %d,%s ", i, r.getName()))
                                .build())
                        .toList()
        );
    }

    @Override
    public Flux<ReactiveHelloResponse> streamGreet(Flux<ReactiveHelloRequest> request) {
        return request.flatMap(r -> Mono.just(
                        ReactiveHelloResponse.newBuilder()
                                .setMessage(String.format("Hello ,%s ", r.getName()))
                                .build()
                )
        );
    }

    @GRpcExceptionHandler
    public Status handle(Exception ex, GRpcExceptionScope scope) {
        var status = Status.INVALID_ARGUMENT.withDescription(ex.getLocalizedMessage()).withCause(ex);
        log.error("(GrpcExceptionAdvice) : ", ex);
        return status;
    }
}
