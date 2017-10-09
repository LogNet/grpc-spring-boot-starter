package org.lognet.springboot.grpc.demo;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Created by 310242212 on 01-Nov-16.
 */

@Aspect
@Component
@Profile(value = {"aopTest"})
public class AopServiceMonitor {

    public AopServiceMonitor() {
    }

    //@AfterReturning("execution(* org.lognet..*Service.*(..))")
    @After("execution(* org.lognet..*Service.*(..))")
    public void logServiceAccess( ) {
        System.out.println("Hi from AOP.");
    }






}