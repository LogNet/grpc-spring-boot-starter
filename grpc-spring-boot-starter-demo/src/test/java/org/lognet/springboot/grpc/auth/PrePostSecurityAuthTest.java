package org.lognet.springboot.grpc.auth;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.CalculatorOuterClass;
import io.grpc.examples.SecuredCalculatorGrpc;
import io.grpc.examples.tasks.Assignment;
import io.grpc.examples.tasks.Person;
import io.grpc.examples.tasks.TaskServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.demo.DemoAppConfiguration;
import org.lognet.springboot.grpc.demo.ITaskService;
import org.lognet.springboot.grpc.demo.NotSpringBeanInterceptor;
import org.lognet.springboot.grpc.security.AuthCallCredentials;
import org.lognet.springboot.grpc.security.AuthHeader;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest(classes = DemoApp.class)
@RunWith(SpringRunner.class)
@Import({PrePostSecurityAuthTest.TestCfg.class})
public class PrePostSecurityAuthTest extends GrpcServerTestBase {

    static class AggregatingStreamObserver<T> implements StreamObserver<T> {
        private final CompletableFuture<List<T>> completion = new CompletableFuture<>();
        private final List<T> allAssignments = new ArrayList<>();

        @Override
        public void onNext(T value) {
            allAssignments.add(value);
        }

        @Override
        public void onError(Throwable t) {
            completion.completeExceptionally(t);
        }

        @Override
        public void onCompleted() {
            completion.complete(allAssignments);
        }

        public List<T> get(Duration duration) throws Throwable {
            try {
                return completion.get(duration.toMillis(), TimeUnit.MILLISECONDS);
            } catch (ExecutionException exception) {
                throw exception.getCause();
            }

        }
    }

    @TestConfiguration
    static class TestCfg extends GrpcSecurityConfigurerAdapter {
        @GRpcService(interceptors = NotSpringBeanInterceptor.class)
        @PreAuthorize("isAuthenticated()")
        public static class SecuredCalculatorService extends SecuredCalculatorGrpc.SecuredCalculatorImplBase{
            @Override
            public void calculate(CalculatorOuterClass.CalculatorRequest request, StreamObserver<CalculatorOuterClass.CalculatorResponse> responseObserver) {
                responseObserver.onNext(DemoAppConfiguration.CalculatorService.calculate(request));
                responseObserver.onCompleted();


            }
        }
        @Override
        public void configure(GrpcSecurity builder) throws Exception {
            builder.authorizeRequests()
                    .withSecuredAnnotation()
                    .userDetailsService(new InMemoryUserDetailsManager(
                            User.withDefaultPasswordEncoder()
                                    .username("user1")
                                    .password("user1")
                                    .authorities("1")
                                    .build(),
                            User.withDefaultPasswordEncoder()
                                    .username("user2")
                                    .password("user2")
                                    .authorities("2")
                                    .build()
                    ));
        }
    }


    private final Person sam = Person.newBuilder()
            .setName("Sam")
            .setAge(13)
            .build();

    private final Person frodo = Person.newBuilder()
            .setName("Frodo")
            .setAge(11)
            .build();

    private final Assignment noopAssigment = Assignment.newBuilder()
            .setDescription("")
            .build();

    private final Assignment saveTheWorld = Assignment.newBuilder()
            .setDescription("Save the world")
            .build();
    private final Assignment keepTheRing = Assignment.newBuilder()
            .setDescription("Keep the ring")
            .build();

    @MockBean
    private ITaskService service;

    @Test
    public void preAuthAnnotationOnClassTest() {


        final SecuredCalculatorGrpc.SecuredCalculatorBlockingStub stub = SecuredCalculatorGrpc
                .newBlockingStub(selectedChanel);

        final CalculatorOuterClass.CalculatorResponse response = stub
                .withCallCredentials(user2Credentials())
                .calculate(CalculatorOuterClass.CalculatorRequest.newBuilder()
                        .setNumber1(1)
                        .setNumber2(1)
                        .setOperation(CalculatorOuterClass.CalculatorRequest.OperationType.ADD)
                        .build());
        assertThat(response.getResult(),Matchers.is(2d));

        final StatusRuntimeException statusRuntimeException = Assert.assertThrows(StatusRuntimeException.class, () -> {
            stub.withCallCredentials(unAuthUserCredentials())
                    .calculate(CalculatorOuterClass.CalculatorRequest.newBuilder()
                            .setNumber1(1)
                            .setNumber2(1)
                            .setOperation(CalculatorOuterClass.CalculatorRequest.OperationType.ADD)
                            .build());
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.UNAUTHENTICATED));
    }

    @Test
    public void unaryPreAuthorizeCallTest() {
        final TaskServiceGrpc.TaskServiceBlockingStub stub = TaskServiceGrpc.newBlockingStub(getChannel())
                .withCallCredentials(user1Credentials());

        Mockito.when(service.findAssignment(frodo))
                .thenReturn(saveTheWorld);

        final Assignment assignment = stub.findAssignmentUnary(frodo);

        assertThat(assignment, Matchers.is(saveTheWorld));

        final StatusRuntimeException statusRuntimeException = Assert.assertThrows(StatusRuntimeException.class, () -> {
            stub.findAssignmentUnary(sam);
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.PERMISSION_DENIED));

        Mockito.verify(service, Mockito.only()).findAssignment(frodo);
    }
    @Test
    public void unaryPreAuthorizeAccessDeniedCallTest() {
        final TaskServiceGrpc.TaskServiceBlockingStub unAuthStub = TaskServiceGrpc.newBlockingStub(getChannel())
                .withCallCredentials(user2Credentials());

        Mockito.when(service.findAssignment(frodo))
                .thenReturn(saveTheWorld);


        final StatusRuntimeException statusRuntimeException = Assert.assertThrows(StatusRuntimeException.class, () -> {
            unAuthStub.findAssignmentUnary(frodo);
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.PERMISSION_DENIED));

        Mockito.verify(service, Mockito.never()).findAssignment(frodo);
    }
    @Test
    public void unaryPreAuthorizeUnAuthCallTest() {
        final TaskServiceGrpc.TaskServiceBlockingStub unAuthStub = TaskServiceGrpc.newBlockingStub(getChannel())
                .withCallCredentials(unAuthUserCredentials());

        Mockito.when(service.findAssignment(frodo))
                .thenReturn(saveTheWorld);


        final StatusRuntimeException statusRuntimeException = Assert.assertThrows(StatusRuntimeException.class, () -> {
            unAuthStub.findAssignmentUnary(frodo);
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.UNAUTHENTICATED));

        Mockito.verify(service, Mockito.never()).findAssignment(frodo);
    }

    @Test
    public void unaryPostAuthorizeCallTest() {
        final TaskServiceGrpc.TaskServiceBlockingStub stub = TaskServiceGrpc.newBlockingStub(getChannel())
                .withCallCredentials(user1Credentials());

        Mockito.when(service.findAssignment(frodo))
                .thenReturn(keepTheRing)
                .thenReturn(noopAssigment);

        final Assignment assignment = stub.findAssignmentUnary(frodo);
        assertThat(assignment, Matchers.is(keepTheRing));

        final StatusRuntimeException statusRuntimeException = Assert.assertThrows(StatusRuntimeException.class, () -> {
            stub.findAssignmentUnary(frodo);
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.PERMISSION_DENIED));

        Mockito.verify(service, Mockito.times(2)).findAssignment(frodo);

    }

    @Test
    public void bidiStreamPrePostAuthorizeOkCallTest() throws Throwable {
        final TaskServiceGrpc.TaskServiceStub stub = TaskServiceGrpc.newStub(getChannel())
                .withCallCredentials(user1Credentials());

        Mockito.when(service.findAssignment(frodo))
                .thenReturn(saveTheWorld)
                .thenReturn(keepTheRing);


        final AggregatingStreamObserver<Assignment> responseObserver = new AggregatingStreamObserver<>();
        final StreamObserver<Person> personsIn = stub.findAssignmentsBidiStream(responseObserver);
        personsIn.onNext(frodo);
        personsIn.onNext(frodo);
        personsIn.onCompleted();

        final List<Assignment> response = responseObserver.get(Duration.ofSeconds(10));
        assertThat(response, Matchers.contains(saveTheWorld, keepTheRing));
        Mockito.verify(service, Mockito.times(2)).findAssignment(frodo);


    }

    @Test
    public void bidiStreamPreAuthorizeFailCallTest() {
        final TaskServiceGrpc.TaskServiceStub stub = TaskServiceGrpc.newStub(getChannel())
                .withCallCredentials(user1Credentials());

        Mockito.when(service.findAssignment(sam))
                .thenReturn(keepTheRing);


        final StatusRuntimeException statusRuntimeException = Assert.assertThrows(StatusRuntimeException.class, () -> {

            final AggregatingStreamObserver<Assignment> observer = new AggregatingStreamObserver<>();
            final StreamObserver<Person> personsIn = stub.findAssignmentsBidiStream(observer);
            personsIn.onNext(sam);
            personsIn.onCompleted();
            observer.get(Duration.ofSeconds(10));
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.PERMISSION_DENIED));
        Mockito.verifyZeroInteractions(service);

    }

    @Test
    public void outStreamPrePostAuthorizeOkCallTest() {
        final TaskServiceGrpc.TaskServiceBlockingStub stub = TaskServiceGrpc.newBlockingStub(getChannel())
                .withCallCredentials(user1Credentials());

        Mockito.when(service.findAssignment(frodo))
                .thenReturn(saveTheWorld)
                .thenReturn(keepTheRing);

        final Iterator<Assignment> assignments = stub.findAssignmentOutStream(frodo);
        List<Assignment> assignmentsList = new ArrayList<>();
        assignments.forEachRemaining(assignmentsList::add);

        assertThat(assignmentsList, Matchers.contains(saveTheWorld, keepTheRing));
        Mockito.verify(service, Mockito.times(2)).findAssignment(frodo);

    }

    @Test
    public void outStreamPostAuthorizeCallFailTest() {
        final TaskServiceGrpc.TaskServiceBlockingStub stub = TaskServiceGrpc.newBlockingStub(getChannel())
                .withCallCredentials(user1Credentials());

        Mockito.when(service.findAssignment(frodo))
                .thenReturn(saveTheWorld)
                .thenReturn(noopAssigment);

        final StatusRuntimeException statusRuntimeException = Assert.assertThrows(StatusRuntimeException.class, () -> {
            stub.findAssignmentOutStream(frodo)
                    .forEachRemaining(a -> {
                    });
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.PERMISSION_DENIED));

        Mockito.verify(service, Mockito.times(2)).findAssignment(frodo);
    }

    @Test
    public void outStreamPreAuthorizeCallFailTest() {
        final TaskServiceGrpc.TaskServiceBlockingStub stub = TaskServiceGrpc.newBlockingStub(getChannel())
                .withCallCredentials(user1Credentials());

        Mockito.when(service.findAssignment(sam))
                .thenReturn(saveTheWorld)
                .thenReturn(keepTheRing);

        final StatusRuntimeException statusRuntimeException = Assert.assertThrows(StatusRuntimeException.class, () -> {
            stub.findAssignmentOutStream(sam)
                    .forEachRemaining(a -> {
                    });
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.PERMISSION_DENIED));

        Mockito.verify(service, Mockito.never()).findAssignment(sam);
    }


    @Test
    public void inStreamPrePostAuthorizeOkCallTest() throws Throwable {
        final TaskServiceGrpc.TaskServiceStub stub = TaskServiceGrpc.newStub(getChannel())
                .withCallCredentials(user1Credentials());

        Mockito.when(service.findAssignment(frodo))
                .thenReturn(saveTheWorld)
                .thenReturn(keepTheRing);

        final AggregatingStreamObserver<Assignment> responseObserver = new AggregatingStreamObserver<>();
        final StreamObserver<Person> personsIn = stub.findAssignmentInStream(responseObserver);
        personsIn.onNext(frodo);
        personsIn.onNext(frodo);
        personsIn.onCompleted();

        final List<Assignment> response = responseObserver.get(Duration.ofSeconds(10));

        assertThat(response, Matchers.hasSize(1));
        assertThat(response.get(0).getDescription(), Matchers.is(
                Stream.of(saveTheWorld, keepTheRing)
                        .map(Assignment::getDescription)
                        .collect(Collectors.joining(System.lineSeparator()))
        ));
        Mockito.verify(service, Mockito.times(2)).findAssignment(frodo);


    }
    @Test
    public void inStreamPreAuthorizeFailCallTest()  {
        final TaskServiceGrpc.TaskServiceStub stub = TaskServiceGrpc.newStub(getChannel())
                .withCallCredentials(user1Credentials());

        Mockito.when(service.findAssignment(frodo))
                .thenReturn(saveTheWorld);

        Mockito.when(service.findAssignment(sam))
                .thenReturn(keepTheRing);


        final StatusRuntimeException statusRuntimeException = Assert.assertThrows(StatusRuntimeException.class, () -> {
            final AggregatingStreamObserver<Assignment> responseObserver = new AggregatingStreamObserver<>();
            final StreamObserver<Person> personsIn = stub.findAssignmentInStream(responseObserver);
            personsIn.onNext(frodo);
            personsIn.onNext(sam);
            personsIn.onCompleted();
            responseObserver.get(Duration.ofSeconds(10));
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.PERMISSION_DENIED));

        Mockito.verify(service, Mockito.only()).findAssignment(frodo);

    }

    @Test
    public void inStreamPostAuthorizeFailCallTest()   {
        final TaskServiceGrpc.TaskServiceStub stub = TaskServiceGrpc.newStub(getChannel())
                .withCallCredentials(user1Credentials());

        Mockito.when(service.findAssignment(frodo))
                .thenReturn(noopAssigment);


        final StatusRuntimeException statusRuntimeException = Assert.assertThrows(StatusRuntimeException.class, () -> {
            final AggregatingStreamObserver<Assignment> responseObserver = new AggregatingStreamObserver<>();
            final StreamObserver<Person> personsIn = stub.findAssignmentInStream(responseObserver);
            personsIn.onNext(frodo);
            personsIn.onCompleted();
            responseObserver.get(Duration.ofSeconds(10));
        });
        assertThat(statusRuntimeException.getStatus().getCode(), Matchers.is(Status.Code.PERMISSION_DENIED));

        Mockito.verify(service, Mockito.only()).findAssignment(frodo);

    }

    private AuthCallCredentials user1Credentials() {
        return new AuthCallCredentials(
                AuthHeader.builder()
                        .basic("user1", "user1".getBytes(StandardCharsets.UTF_8))
        );
    }
    private AuthCallCredentials user2Credentials() {
        return new AuthCallCredentials(
                AuthHeader.builder()
                        .basic("user2", "user2".getBytes(StandardCharsets.UTF_8))
        );
    }
    private AuthCallCredentials unAuthUserCredentials() {
        return new AuthCallCredentials(
                AuthHeader.builder()
                        .basic("dummy", "dummy".getBytes(StandardCharsets.UTF_8))
        );
    }
}
