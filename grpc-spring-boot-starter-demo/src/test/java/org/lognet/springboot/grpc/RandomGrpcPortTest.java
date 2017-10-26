package org.lognet.springboot.grpc;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.context.LocalRunningGrpcPort;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutionException;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class},webEnvironment = NONE,
properties = "grpc.port=0"
)
public class RandomGrpcPortTest extends GrpcServerTestBase {

    @Value("${local.grpc.port}")
    int port;

    @LocalRunningGrpcPort
    int runningPort;

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected void beforeGreeting() {
        Assert.assertEquals(0,gRpcServerProperties.getPort());
        Assert.assertEquals(port,runningPort);
    }
}
