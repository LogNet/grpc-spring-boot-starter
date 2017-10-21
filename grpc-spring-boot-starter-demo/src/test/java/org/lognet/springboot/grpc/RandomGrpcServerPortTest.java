package org.lognet.springboot.grpc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * Created by Dominik Sandjaja
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE, properties = {"grpc.enabled=true", "grpc.port=0"})
public class RandomGrpcServerPortTest extends GrpcServerTestBase {

    @Value("{local.grpc.port")
    private String portFromValue;

    @Test
    public void randomPortShouldBeSet() throws Throwable {
        assertNotEquals("0", portFromValue);
    }

}
