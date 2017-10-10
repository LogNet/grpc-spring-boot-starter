package org.lognet.springboot.grpc.demo;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Created by 310242212 on 01-Nov-16.
 */

@Aspect
@Component
@Profile(value = {"aopTest"})
@Slf4j
public class AopServiceMonitor {

    public AopServiceMonitor() {
    }

    //@AfterReturning("execution(* org.lognet..*Service.*(..))")
    @After("execution(* org.lognet..*Service.*(..))")
    public void afterLogServiceAccess( ) {

        log.info("Hi from AOP. - after");
    }

    @Before("execution(* org.lognet..*Service.*(..))")
    public void beforeLogServiceAccess( ) {
        log.info("Hi from AOP. - before");
    }






}