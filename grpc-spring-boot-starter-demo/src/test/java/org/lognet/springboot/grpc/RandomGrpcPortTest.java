package org.lognet.springboot.grpc;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE, properties = {"grpc.port=0","grpc.shutdownGrace=-1"})
public class RandomGrpcPortTest extends GrpcServerTestBase {

    @Override
    protected void beforeGreeting() {
        Assert.assertEquals(0,gRpcServerProperties.getPort().intValue());

    }
}
