package org.lognet.springboot.grpc;

import io.grpc.examples.GreeterGrpc;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.context.LocalRunningGrpcPort;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.TestSocketUtils;


import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE)
@ActiveProfiles({"disable-security","default-port"})
@DirtiesContext
public class EnvVarGrpcPortTest extends GrpcServerTestBase {


    private static int randomPort;
    private static EnvironmentVariables environmentVariables;

    @ClassRule
    public static EnvironmentVariables environmentVariables() {
        randomPort = TestSocketUtils.findAvailableTcpPort();

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
    protected GreeterGrpc.GreeterFutureStub beforeGreeting(GreeterGrpc.GreeterFutureStub stub) {
        Assert.assertEquals(randomPort, gRpcServerProperties.getPort().intValue());
        Assert.assertEquals(randomPort, runningPort);
        return stub;
    }
}
