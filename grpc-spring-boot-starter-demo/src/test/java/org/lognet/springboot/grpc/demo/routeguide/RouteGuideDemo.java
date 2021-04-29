package org.lognet.springboot.grpc.demo.routeguide;

import io.grpc.Status;
import io.grpc.examples.routeguide.Point;
import io.grpc.examples.routeguide.RouteGuideGrpc;
import io.grpc.examples.routeguide.RouteNote;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.concurrent.ListenableFuture;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DemoApp.class, webEnvironment = NONE)
public class RouteGuideDemo extends GrpcServerTestBase {
    @Test
    public void bidirectionalStreamingDemo() throws ExecutionException, InterruptedException, TimeoutException {
        final RouteGuideGrpc.RouteGuideStub asyncStub = RouteGuideGrpc.newStub(getChannel());


        final CompletableFuture<Boolean> completion = new CompletableFuture<>();


        final StreamObserver<RouteNote> liveResponseObserver = new StreamObserver<RouteNote>() {
            @Override
            public void onNext(RouteNote note) {
                log.info("Got message \"{}\" at {}, {}", note.getMessage(), note.getLocation()
                        .getLatitude(), note.getLocation().getLongitude());

            }

            @Override
            public void onError(Throwable t) {
                log.warn("RouteChat Failed: {}", Status.fromThrowable(t));
                completion.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                log.info("Finished RouteChat");
                completion.complete(true);
            }
        };
        StreamObserver<RouteNote> responseObserver = Mockito.spy(liveResponseObserver);

        StreamObserver<RouteNote> requestObserver =asyncStub.routeChat(responseObserver);

        final int timeout = 1000;

        try {
            Point p1 = Point.newBuilder().setLongitude(1).setLatitude(1).build();
            Point p2 = Point.newBuilder().setLongitude(2).setLatitude(2).build();
            RouteNote n1 = RouteNote.newBuilder().setLocation(p1).setMessage("m1").build();
            RouteNote n2 = RouteNote.newBuilder().setLocation(p2).setMessage("m2").build();
            RouteNote n3 = RouteNote.newBuilder().setLocation(p1).setMessage("m3").build();
            RouteNote n4 = RouteNote.newBuilder().setLocation(p2).setMessage("m4").build();
            RouteNote n5 = RouteNote.newBuilder().setLocation(p1).setMessage("m5").build();
            RouteNote n6 = RouteNote.newBuilder().setLocation(p1).setMessage("m6").build();
            int timesOnNext = 0;

            verify(responseObserver, never()).onNext(any(RouteNote.class));

            requestObserver.onNext(n1);
            verify(responseObserver, never()).onNext(any(RouteNote.class));

            requestObserver.onNext(n2);
            verify(responseObserver, never()).onNext(any(RouteNote.class));

            requestObserver.onNext(n3);
            ArgumentCaptor<RouteNote> routeNoteCaptor = ArgumentCaptor.forClass(RouteNote.class);
            verify(responseObserver, timeout(timeout).times(++timesOnNext)).onNext(routeNoteCaptor.capture());
            RouteNote result = routeNoteCaptor.getValue();
            assertEquals(p1, result.getLocation());
            assertEquals("m1", result.getMessage());

            requestObserver.onNext(n4);
            routeNoteCaptor = ArgumentCaptor.forClass(RouteNote.class);

            verify(responseObserver, timeout(timeout).times(++timesOnNext)).onNext(routeNoteCaptor.capture());
            result = routeNoteCaptor.getAllValues().get(timesOnNext - 1);
            assertEquals(p2, result.getLocation());
            assertEquals("m2", result.getMessage());

            requestObserver.onNext(n5);
            routeNoteCaptor = ArgumentCaptor.forClass(RouteNote.class);
            timesOnNext += 2;
            verify(responseObserver, timeout(timeout).times(timesOnNext)).onNext(routeNoteCaptor.capture());
            result = routeNoteCaptor.getAllValues().get(timesOnNext - 2);
            assertEquals(p1, result.getLocation());
            assertEquals("m1", result.getMessage());
            result = routeNoteCaptor.getAllValues().get(timesOnNext - 1);
            assertEquals(p1, result.getLocation());
            assertEquals("m3", result.getMessage());

            requestObserver.onNext(n6);
            routeNoteCaptor = ArgumentCaptor.forClass(RouteNote.class);
            timesOnNext += 3;
            verify(responseObserver, timeout(timeout).times(timesOnNext)).onNext(routeNoteCaptor.capture());
            result = routeNoteCaptor.getAllValues().get(timesOnNext - 3);
            assertEquals(p1, result.getLocation());
            assertEquals("m1", result.getMessage());
            result = routeNoteCaptor.getAllValues().get(timesOnNext - 2);
            assertEquals(p1, result.getLocation());
            assertEquals("m3", result.getMessage());
            result = routeNoteCaptor.getAllValues().get(timesOnNext - 1);
            assertEquals(p1, result.getLocation());
            assertEquals("m5", result.getMessage());

            // Mark the end of requests
            requestObserver.onCompleted();
            verify(responseObserver, timeout(timeout)).onCompleted();
            verify(responseObserver, never()).onError(any(Throwable.class));


        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }




        assertThat("Should be completed successfully",completion.get(5, TimeUnit.SECONDS));
    }


}
