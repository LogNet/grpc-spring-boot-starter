package org.lognet.springboot.grpc;


import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

@DirtiesContext
public class ConfigServerEnvironmentTest2 extends ConfigServerEnvironmentBaseTest{


    @BeforeClass
    public static void startConfigServer( ) throws IOException, URISyntaxException {
        Properties properties = new Properties();
        properties.put("grpc.port","0");
        properties.put("grpc.shutdownGrace","-1");
        startConfigServer(properties);

    }




    @Test
    public void assertConfigServerConfiguredPort(){

        Assert.assertNotEquals(runningPort,gRpcServerProperties.getPort().intValue());
        Assert.assertNotEquals(GRpcServerProperties.DEFAULT_GRPC_PORT,getPort());
        Assert.assertEquals(0,gRpcServerProperties.getPort().intValue());
    }


}
