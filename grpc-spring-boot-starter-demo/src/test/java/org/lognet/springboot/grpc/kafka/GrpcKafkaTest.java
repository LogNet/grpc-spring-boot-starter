package org.lognet.springboot.grpc.kafka;

import io.grpc.examples.custom.Custom;
import io.grpc.examples.custom.CustomServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE)
@ActiveProfiles({"disable-security", "kafka-test"})
@Import({GrpcKafkaTest.TestConfig.class})
public class GrpcKafkaTest extends GrpcServerTestBase {

    @Configuration
    @Slf4j
    public static class TestConfig {
        @Autowired
        private KafkaTemplate<String, byte[]> kafkaTemplate;

        @GRpcService
        class MyCustomService extends io.grpc.examples.custom.CustomServiceGrpc.CustomServiceImplBase {
            @Override
            public void custom(Custom.CustomRequest request, StreamObserver<Custom.CustomReply> responseObserver) {
                kafkaTemplate.send(kafkaTemplate.getDefaultTopic(), request.getName().getBytes())
                        .addCallback(e -> {
                            responseObserver.onNext(Custom.CustomReply.newBuilder().setMessage(request.getName()).build());
                            responseObserver.onCompleted();
                        }, f -> {
                            responseObserver.onError(f);
                        });
            }
        }


        @MockBean
        public Consumer<String> consumerMock;

        @Bean
        public Consumer<String> consumer() {
            return consumerMock;
        }
    }

    @Autowired
    public Consumer<String> consumerMock;

    @Test
    public void sendCustomMessage() {
        String name = "Johnny";
        final Custom.CustomReply customReply = CustomServiceGrpc.newBlockingStub(getChannel())
                .custom(Custom.CustomRequest.newBuilder()
                        .setName(name)
                        .build()
                );
        assertThat(customReply.getMessage(), Matchers.is(name));
        Mockito.verify(consumerMock,
                Mockito.timeout(Duration.ofSeconds(3).toMillis()).times(1)
        ).accept(name);
    }
}
