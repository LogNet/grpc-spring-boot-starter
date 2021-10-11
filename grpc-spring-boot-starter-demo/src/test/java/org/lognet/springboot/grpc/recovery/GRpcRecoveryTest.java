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
@Import(GRpcRecoveryTest.Cfg.class)
public class GRpcRecoveryTest extends GrpcServerTestBase {

    static class CheckedException extends Exception {

    }
    static class CheckedException1 extends Exception {

    }

    static class ExceptionA extends RuntimeException {

    }

    static class ExceptionB extends ExceptionA {

    }

    static class Exception1 extends RuntimeException {

    }

    @TestConfiguration
    static class Cfg {


        @GRpcServiceAdvice
        static class CustomErrorHandler {
            @GRpcExceptionHandler
            public Status handleA(ExceptionA e, GRpcExceptionScope scope) {
                return Status.NOT_FOUND;
            }

            @GRpcExceptionHandler
            public Status handleB(ExceptionB e, GRpcExceptionScope scope) {
                return Status.ALREADY_EXISTS;
            }


            @GRpcExceptionHandler
            public Status handleCheckedException(CheckedException e, GRpcExceptionScope scope) {
                return Status.OUT_OF_RANGE;
            }

            @GRpcExceptionHandler
            public Status handle(Exception e, GRpcExceptionScope scope) {
                return Status.DATA_LOSS;
            }
        }


        @GRpcService
        static class CustomService extends CustomServiceGrpc.CustomServiceImplBase {

            @Override
            public void custom(Custom.CustomRequest request, StreamObserver<Custom.CustomReply> responseObserver) {
                super.custom(request, responseObserver);
            }

            @Override
            public StreamObserver<Custom.CustomRequest> customStream(StreamObserver<Custom.CustomReply> responseObserver) {

                return new StreamObserver<Custom.CustomRequest>() {
                    @Override
                    public void onNext(Custom.CustomRequest value) {
                        responseObserver.onNext(Custom.CustomReply.newBuilder().build());
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onCompleted() {
                        throw new GRpcRuntimeExceptionWrapper(new CheckedException1());
                    }
                };
            }

            @GRpcExceptionHandler
            public Status handle(CheckedException1 e, GRpcExceptionScope scope) {
                return Status.RESOURCE_EXHAUSTED;
            }

            @GRpcExceptionHandler
            public Status handleB(ExceptionB e, GRpcExceptionScope scope) {

                assertThat(scope.getResponseHeaders(), notNullValue());
                scope.getResponseHeaders().put(Metadata.Key.of("test", Metadata.ASCII_STRING_MARSHALLER), "5");

                assertThat(scope.getRequest(), notNullValue());
                assertThat(scope.getRequest(), isA(Custom.CustomRequest.class));

                assertThat(scope.getCallHeaders(), notNullValue());
                assertThat(scope.getMethodCallAttributes(), notNullValue());
                assertThat(scope.getMethodDescriptor(), notNullValue());


                return Status.FAILED_PRECONDITION;
            }


        }

    }

    @SpyBean
    private Cfg.CustomService srv;

    @SpyBean
    private Cfg.CustomErrorHandler handler;


    @Test
    public void streamingServiceErrorHandlerTest() throws ExecutionException, InterruptedException {



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





        final Throwable actual = errorFuture.get();
        assertThat(actual, notNullValue());
        assertThat(actual, isA(StatusRuntimeException.class));
        assertThat(((StatusRuntimeException)actual).getStatus(), is(Status.RESOURCE_EXHAUSTED));

        Mockito.verify(srv,times(1)).handle(any(CheckedException1.class),any());

    }

    @Test
    public void checkedExceptionHandlerTest() {
        Mockito.doThrow(new GRpcRuntimeExceptionWrapper(new CheckedException()))
                .when(srv)
                .custom(any(Custom.CustomRequest.class), any(StreamObserver.class));

        final StatusRuntimeException statusRuntimeException = assertThrows(StatusRuntimeException.class, () ->
                CustomServiceGrpc.newBlockingStub(getChannel()).custom(Custom.CustomRequest.newBuilder().build())
        );
        assertThat(statusRuntimeException.getStatus(), is(Status.OUT_OF_RANGE));

        Mockito.verify(srv, never()).handleB(any(), any());


        Mockito.verify(handler, never()).handle(any(), any());
        Mockito.verify(handler, never()).handleA(any(), any());
        Mockito.verify(handler, times(1)).handleCheckedException(any(CheckedException.class), any());
        Mockito.verify(handler, never()).handleB(any(), any());


    }

    @Test
    public void globalHandlerTest() {
        Mockito.doThrow(ExceptionA.class)
                .when(srv)
                .custom(any(Custom.CustomRequest.class), any(StreamObserver.class));

        final StatusRuntimeException statusRuntimeException = assertThrows(StatusRuntimeException.class, () ->
                CustomServiceGrpc.newBlockingStub(getChannel()).custom(Custom.CustomRequest.newBuilder().build())
        );
        assertThat(statusRuntimeException.getStatus(), is(Status.NOT_FOUND));

        Mockito.verify(srv, never()).handleB(any(), any());


        Mockito.verify(handler, never()).handle(any(), any());
        Mockito.verify(handler, times(1)).handleA(any(), any());
        Mockito.verify(handler, never()).handleB(any(), any());


    }

    @Test
    public void globalHandlerWithExceptionHierarchyTest() {
        Mockito.doThrow(Exception1.class)
                .when(srv)
                .custom(any(Custom.CustomRequest.class), any(StreamObserver.class));

        final StatusRuntimeException statusRuntimeException = assertThrows(StatusRuntimeException.class, () ->
                CustomServiceGrpc.newBlockingStub(getChannel()).custom(Custom.CustomRequest.newBuilder().build())
        );
        assertThat(statusRuntimeException.getStatus(), is(Status.DATA_LOSS));

        Mockito.verify(srv, never()).handleB(any(), any());


        Mockito.verify(handler, never()).handleA(any(), any());
        Mockito.verify(handler, never()).handleB(any(), any());
        Mockito.verify(handler, times(1)).handle(any(), any());


    }

    @Test
    public void privateHandlerHasHigherPrecedence() {
        Mockito.doThrow(ExceptionB.class)
                .when(srv)
                .custom(any(Custom.CustomRequest.class), any(StreamObserver.class));

        final StatusRuntimeException statusRuntimeException = assertThrows(StatusRuntimeException.class, () ->
                CustomServiceGrpc.newBlockingStub(getChannel()).custom(Custom.CustomRequest.newBuilder().build())
        );
        assertThat(statusRuntimeException.getStatus(), is(Status.FAILED_PRECONDITION));

        Mockito.verify(srv, times(1)).handleB(any(), any());

        final String testTrailer = statusRuntimeException.getTrailers().get(Metadata.Key.of("test", Metadata.ASCII_STRING_MARSHALLER));
        assertThat(testTrailer, is("5"));


        Mockito.verify(handler, never()).handle(any(), any());
        Mockito.verify(handler, never()).handleA(any(), any());
        Mockito.verify(handler, never()).handleB(any(), any());

    }
}
