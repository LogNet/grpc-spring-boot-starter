package org.lognet.springboot.grpc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.demo.GreeterService;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * Created by alexf on 28-Jan-16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class },webEnvironment = NONE
        ,properties = {"spring.aop.proxy-target-class=true","grpc.port=6666"}
)
@ActiveProfiles(profiles = {"aopTest"})
public class DemoAppTestAop {


    @Autowired
    private GreeterService greeterService;

    @Autowired
    private DemoApp.CalculatorService calculatorService;



    @Test
    public void simpleAopTest() throws ExecutionException, InterruptedException {

        assertTrue(AopUtils.isAopProxy(greeterService));
        assertTrue(AopUtils.isAopProxy(calculatorService));

    }




}
