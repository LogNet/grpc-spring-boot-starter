package org.lognet.springboot.grpc.demo;


import io.grpc.examples.tasks.Assignment;
import io.grpc.examples.tasks.Person;

public interface ITaskService {
     Assignment findAssignment(Person person);
}
