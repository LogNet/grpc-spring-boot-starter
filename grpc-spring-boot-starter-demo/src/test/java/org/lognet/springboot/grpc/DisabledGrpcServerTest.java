package org.lognet.springboot.grpc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * Created by alexf on 28-Jan-16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class },webEnvironment = NONE
        ,properties = {"grpc.enabled=false","grpc.inProcessServerName=testServer","grpc.shutdownGrace=-1"}
)
public class DisabledGrpcServerTest extends GrpcServerTestBase {






    @Test
    public void disabledServerTest() throws Throwable {
        assertNull(grpcServerRunner);
        assertNotNull(grpcInprocessServerRunner);

    }




}
