package org.lognet.springboot.grpc.demo;

import io.grpc.examples.reactor.ReactiveHelloRequest;
import io.grpc.examples.reactor.ReactiveHelloResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ReactiveGreeterService {
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
