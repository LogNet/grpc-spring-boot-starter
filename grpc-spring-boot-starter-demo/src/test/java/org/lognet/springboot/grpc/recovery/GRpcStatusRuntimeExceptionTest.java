package org.lognet.springboot.grpc.recovery;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.custom.Custom;
import io.grpc.examples.custom.CustomServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE)
@ActiveProfiles({"disable-security"})
@Import(GRpcStatusRuntimeExceptionTest.Cfg.class)
public class GRpcStatusRuntimeExceptionTest extends GrpcServerTestBase {


    @TestConfiguration
    static class Cfg {

        @GRpcService
        static class CustomService extends CustomServiceGrpc.CustomServiceImplBase {

            @Override
            public void anotherCustom(Custom.CustomRequest request, StreamObserver<Custom.CustomReply> responseObserver) {
                if ("NPE".equals(request.getName())) {
                    ExceptionUtilsKt.throwException(new NullPointerException());
                }
                if ("RT_ERROR".equals(request.getName())) {
                    responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
                }
                if ("ERROR".equals(request.getName())) {
                    responseObserver.onError(Status.OUT_OF_RANGE.asException());
                } else {
                    ExceptionUtilsKt.throwException(Status.FAILED_PRECONDITION);
                }
            }

            @Override
            public void custom(Custom.CustomRequest request, StreamObserver<Custom.CustomReply> responseObserver) {
                throw new StatusRuntimeException(Status.FAILED_PRECONDITION);

            }

            @Override
            public StreamObserver<Custom.CustomRequest> customStream(StreamObserver<Custom.CustomReply> responseObserver) {
                throw new StatusRuntimeException(Status.FAILED_PRECONDITION);
            }

            @GRpcExceptionHandler
            public Status handle(NullPointerException e, GRpcExceptionScope scope) {
                return Status.DATA_LOSS;
            }

        }

    }

    @Test
    public void streamingStatusRuntimeExceptionTest() throws ExecutionException, InterruptedException, TimeoutException {
        final CompletableFuture<Throwable> errorFuture = new CompletableFuture<>();
        final StreamObserver<Custom.CustomReply> reply = new StreamObserver<Custom.CustomReply>() {

            @Override
            public void onNext(Custom.CustomReply value) {

            }

            @Override
            public void onError(Throwable t) {
                errorFuture.complete(t);
            }

            @Override
            public void onCompleted() {
                errorFuture.complete(null);
            }
        };

        final StreamObserver<Custom.CustomRequest> requests = CustomServiceGrpc.newStub(getChannel()).customStream(reply);
        requests.onNext(Custom.CustomRequest.newBuilder().build());
        requests.onCompleted();

        final Throwable actual = errorFuture.get(20, TimeUnit.SECONDS);
        assertThat(actual, notNullValue());
        assertThat(actual, isA(StatusRuntimeException.class));
        assertThat(((StatusRuntimeException) actual).getStatus(), is(Status.FAILED_PRECONDITION));
    }

    @Test
    public void statusRuntimeExceptionTest() {
        final StatusRuntimeException statusRuntimeException = assertThrows(StatusRuntimeException.class, () ->
                CustomServiceGrpc.newBlockingStub(getChannel()).custom(Custom.CustomRequest.newBuilder().build())
        );
        assertThat(statusRuntimeException.getStatus(), is(Status.FAILED_PRECONDITION));
    }

    @Test
    public void statusExceptionTest() {
        final StatusRuntimeException statusRuntimeException = assertThrows(StatusRuntimeException.class, () ->
                CustomServiceGrpc.newBlockingStub(getChannel()).anotherCustom(Custom.CustomRequest.newBuilder().build())
        );
        assertThat(statusRuntimeException.getStatus(), is(Status.FAILED_PRECONDITION));
    }

    @Test
    public void npeExceptionTest() {
        final StatusRuntimeException statusRuntimeException = assertThrows(StatusRuntimeException.class, () ->
                CustomServiceGrpc.newBlockingStub(getChannel()).anotherCustom(Custom.CustomRequest.newBuilder().setName("NPE").build())
        );
        assertThat(statusRuntimeException.getStatus(), is(Status.DATA_LOSS));
    }

    @Test
    public void errorExceptionTest() {
        final StatusRuntimeException statusRuntimeException = assertThrows(StatusRuntimeException.class, () ->
                CustomServiceGrpc.newBlockingStub(getChannel()).anotherCustom(
                        Custom.CustomRequest.newBuilder().setName("ERROR").build()
                )
        );
        assertThat(statusRuntimeException.getStatus(), is(Status.OUT_OF_RANGE));
    }
    public void rtErrorExceptionTest() {
        final StatusRuntimeException statusRuntimeException = assertThrows(StatusRuntimeException.class, () ->
                CustomServiceGrpc.newBlockingStub(getChannel()).anotherCustom(
                        Custom.CustomRequest.newBuilder().setName("RT_ERROR").build()
                )
        );
        assertThat(statusRuntimeException.getStatus(), is(Status.NOT_FOUND));
    }

}
