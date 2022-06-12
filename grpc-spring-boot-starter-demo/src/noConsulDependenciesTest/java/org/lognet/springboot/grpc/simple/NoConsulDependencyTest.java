package org.lognet.springboot.grpc.simple;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.lognet.springboot.grpc.autoconfigure.consul.GrpcConsulProperties;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE)
@ActiveProfiles("test")
public class NoConsulDependencyTest extends GrpcServerTestBase {

    @Test
    public void noConsulClassesTest() {

        final NoClassDefFoundError error = assertThrows(NoClassDefFoundError.class, () -> {
            try {
                ReflectionUtils.findMethod(GrpcConsulProperties.class, "getDiscovery");
            }catch (IllegalStateException illegalStateException){
                throw illegalStateException.getCause();
            }
        });
        assertThat(error.getMessage(), Matchers.containsString("org/springframework/cloud/consul/discovery/ConsulDiscoveryProperties"));
    }


}
