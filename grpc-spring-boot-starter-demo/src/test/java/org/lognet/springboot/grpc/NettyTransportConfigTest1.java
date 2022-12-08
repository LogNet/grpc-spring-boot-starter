package org.lognet.springboot.grpc;

import io.grpc.examples.GreeterGrpc;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE)
@ActiveProfiles("netty-test1")
public class NettyTransportConfigTest1 extends GrpcServerTestBase {

    @Override
    protected GreeterGrpc.GreeterFutureStub beforeGreeting(GreeterGrpc.GreeterFutureStub stub) {
        Assert.assertNotEquals(getPort(),gRpcServerProperties.getNettyServer().getPrimaryListenAddress().getPort());
        return stub;

    }
}
