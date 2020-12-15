package org.lognet.springboot.grpc.demo;

import io.grpc.examples.GreeterOuterClass;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PersonValidator implements  ConstraintValidator<PersonConstraint, GreeterOuterClass.Person> {


    @Override
    public boolean isValid(GreeterOuterClass.Person value, ConstraintValidatorContext constraintContext) {


        if("bob".equalsIgnoreCase(value.getName())){
            return 20<value.getAge();
        }
        return true;


    }
}
