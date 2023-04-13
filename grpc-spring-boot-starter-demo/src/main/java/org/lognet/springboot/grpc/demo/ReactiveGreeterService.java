package org.lognet.springboot.grpc.demo;

import io.grpc.examples.reactor.ReactiveHelloRequest;
import io.grpc.examples.reactor.ReactiveHelloResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@ConditionalOnClass(Transactional.class)
public class ReactiveGreeterService {
    @Transactional(readOnly = true)
    public Mono<ReactiveHelloResponse> greet(Mono<ReactiveHelloRequest> request) {
        return Mono
                .from(request)
                .flatMap(r -> {
                    if ("wolf".equalsIgnoreCase(r.getName())) {
                        return Mono.error(new Exception("Wolf is not welcome!"));
                    }
                    return Mono.just(ReactiveHelloResponse.newBuilder()
                            .setMessage("Hello " + r.getName())
                            .build());
                });
    }
}
