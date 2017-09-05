package org.lognet.springboot.grpc;

import com.google.common.base.Throwables;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.apache.tomcat.util.ExceptionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.demo.GreeterService;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.ConnectException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * Created by alexf on 28-Jan-16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class },webEnvironment = NONE
        ,properties = {"grpc.enabled=false","grpc.inProcessServerName=testServer"}
)
public class DisabledGrpcServerTest extends GrpcServerTestBase {






    @Before
     public void setup() {
         channel = InProcessChannelBuilder.forName(gRpcServerProperties.getInProcessServerName())
        .usePlaintext(true)
        .build();
        }

     @After
     public void tearDown() {
         channel.shutdown();
     }

    @Test
    public void disabledServerTest() throws Throwable {
        assertNull(grpcServerRunner);
        assertNotNull(grpcInprocessServerRunner);

    }




}
