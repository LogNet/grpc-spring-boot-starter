package org.lognet.springboot.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class, TestConfig.class, GrpcGzipCompressionTest.CompressionTestConfig.class},
        webEnvironment = RANDOM_PORT,
        properties = {
                "grpc.shutdownGrace=-1",
                "spring.main.web-application-type=servlet"
        })
@ActiveProfiles("disable-security")
public class GrpcGzipCompressionTest extends GrpcServerTestBase {

    private static final Metadata.Key<String> GRPC_ENCODING_KEY =
            Metadata.Key.of("grpc-encoding", Metadata.ASCII_STRING_MARSHALLER);

    private static final AtomicReference<String> serverReceivedEncoding = new AtomicReference<>();
    private static final AtomicReference<String> clientReceivedEncoding = new AtomicReference<>();

    @TestConfiguration
    public static class CompressionTestConfig {

        @Bean
        @GRpcGlobalInterceptor
        public ServerInterceptor compressionCapturingInterceptor() {
            return new ServerInterceptor() {
                @Override
                public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                        ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                    serverReceivedEncoding.set(headers.get(GRPC_ENCODING_KEY));
                    call.setCompression("gzip");
                    return next.startCall(call, headers);
                }
            };
        }
    }

    @Test
    public void gzipCompressedRequestShouldSucceed() throws Exception {
        serverReceivedEncoding.set(null);
        clientReceivedEncoding.set(null);

        GreeterGrpc.GreeterFutureStub stub = GreeterGrpc.newFutureStub(selectedChanel)
                .withCompression("gzip");

        GreeterOuterClass.HelloRequest request = GreeterOuterClass.HelloRequest.newBuilder()
                .setName(name)
                .build();

        String reply = stub.sayHello(request).get().getMessage();

        assertNotNull("Reply should not be null", reply);
        assertTrue("Reply should contain the name", reply.contains(name));
        assertEquals("Server should receive gzip encoding header", "gzip", serverReceivedEncoding.get());
    }

    @Test
    public void gzipCompressedResponseShouldBeDecodable() throws Exception {
        clientReceivedEncoding.set(null);

        ClientInterceptor encodingCaptureInterceptor = new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                    MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                        next.newCall(method, callOptions)) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                                responseListener) {
                            @Override
                            public void onHeaders(Metadata headers) {
                                clientReceivedEncoding.set(headers.get(GRPC_ENCODING_KEY));
                                super.onHeaders(headers);
                            }
                        }, headers);
                    }
                };
            }
        };

        Channel interceptedChannel = io.grpc.ClientInterceptors.intercept(selectedChanel, encodingCaptureInterceptor);

        GreeterGrpc.GreeterFutureStub stub = GreeterGrpc.newFutureStub(interceptedChannel)
                .withCompression("gzip");

        GreeterOuterClass.HelloRequest request = GreeterOuterClass.HelloRequest.newBuilder()
                .setName(name)
                .build();

        String reply = stub.sayHello(request).get().getMessage();

        assertNotNull("Reply should not be null", reply);
        assertTrue("Reply should contain the name", reply.contains(name));
        assertEquals("Response should be gzip-encoded", "gzip", clientReceivedEncoding.get());
    }
}
