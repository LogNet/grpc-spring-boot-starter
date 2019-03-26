package org.lognet.springboot.grpc;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.context.LocalRunningGrpcPort;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE)
public class EnvVarGrpcPortTest extends GrpcServerTestBase {


    private static int randomPort;
    private static EnvironmentVariables environmentVariables;

    @ClassRule
    public static EnvironmentVariables environmentVariables() {
        randomPort = SocketUtils.findAvailableTcpPort();

        environmentVariables = new EnvironmentVariables();
        environmentVariables.set("GRPC_PORT", "" + randomPort);
        return environmentVariables;
    }

    @AfterClass
    public static void tearDown() {
        environmentVariables.clear("GRPC_PORT");
    }

    @LocalRunningGrpcPort
    int runningPort;

    @Override
    protected int getPort() {
        return runningPort;
    }

    @Override
    protected void beforeGreeting() {
        Assert.assertEquals(randomPort, gRpcServerProperties.getPort().intValue());
        Assert.assertEquals(randomPort, runningPort);
    }
}
