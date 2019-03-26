package org.lognet.springboot.grpc;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.lognet.springboot.grpc.context.LocalRunningGrpcPort;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE)
public class DefaultGrpcPortTest extends GrpcServerTestBase {
    @LocalRunningGrpcPort
    int runningPort;

    @Override
    protected int getPort() {
        return runningPort;
    }

    @Override
    protected void beforeGreeting() {
        assertThat( gRpcServerProperties.getPort(), CoreMatchers.nullValue(Integer.class));
        Assert.assertEquals(GRpcServerProperties.DEFAULT_GRPC_PORT, runningPort);
    }
}
