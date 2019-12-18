package org.lognet.springboot.grpc;

import org.junit.BeforeClass;
import org.junit.Test;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@DirtiesContext
public class ConfigServerEnvironmentTest2 extends ConfigServerEnvironmentBaseTest{


    @BeforeClass
    public static void startConfigServer() throws IOException, URISyntaxException {
        Properties properties = new Properties();
        properties.put("grpc.port","0");
        properties.put("grpc.shutdownGrace","-1");
        startConfigServer(properties);

    }




    @Test
    public void assertConfigServerConfiguredPort(){

        assertNotEquals(runningPort,gRpcServerProperties.getPort().intValue());
        assertNotEquals(GRpcServerProperties.DEFAULT_GRPC_PORT,getPort());
        assertEquals(0,gRpcServerProperties.getPort().intValue());
    }


}
