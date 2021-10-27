package org.lognet.springboot.grpc.demo;

import io.grpc.examples.tasks.Assignment;
import io.grpc.examples.tasks.Person;
import io.grpc.examples.tasks.TaskServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Optional;

@GRpcService
public class GrpcTaskService extends TaskServiceGrpc.TaskServiceImplBase {


    private ITaskService service;

    @Autowired
    public void setService(Optional<ITaskService> service) {
        this.service = service.orElse(new ITaskService() {
            @Override
            public Assignment findAssignment(Person person) {
                return null;
            }
        });
    }

    @Override
    @PreAuthorize("hasAuthority('1') && #person.age<12")
    @PostAuthorize("returnObject.description.length()>0")
    public void findAssignmentUnary(Person person, StreamObserver<Assignment> responseObserver) {
        final Assignment assignment = service.findAssignment(person);
        responseObserver.onNext(assignment);
        responseObserver.onCompleted();

    }

    @Override
    @PreAuthorize("#p0.age<12")
    @PostAuthorize("returnObject.description.length()>0")
    public StreamObserver<Person> findAssignmentsBidiStream(StreamObserver<Assignment> responseObserver) {
        return new StreamObserver<Person>() {
            @Override
            public void onNext(Person person) {
                final Assignment assignment = service.findAssignment(person);
                responseObserver.onNext(assignment);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    @PreAuthorize("#person.age<12")
    @PostAuthorize("returnObject.description.length()>0")
    public void findAssignmentOutStream(Person person, StreamObserver<Assignment> responseObserver) {
        responseObserver.onNext(service.findAssignment(person));
        responseObserver.onNext(service.findAssignment(person));
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("#p0.getAge()<12")
    @PostAuthorize("returnObject.description.length()>0")
    public StreamObserver<Person> findAssignmentInStream(StreamObserver<Assignment> responseObserver) {
        return new StreamObserver<Person>() {
            private final StringBuilder assignment = new StringBuilder();

            @Override
            public void onNext(Person person) {
                if(0!=assignment.length()){
                    assignment.append(System.lineSeparator());
                }
                assignment.append(service.findAssignment(person).getDescription());
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(Assignment.newBuilder()
                        .setDescription(assignment.toString())
                        .build());
                responseObserver.onCompleted();
            }
        };
    }
}
