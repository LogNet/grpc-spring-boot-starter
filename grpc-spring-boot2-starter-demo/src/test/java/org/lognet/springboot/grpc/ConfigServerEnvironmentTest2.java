package org.lognet.springboot.grpc;

import org.junit.BeforeClass;
import org.junit.Test;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.lognet.springboot.grpc.context.LocalRunningGrpcPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


public class ConfigServerEnvironmentTest2 extends ConfigServerEnvironmentBaseTest{

    @LocalRunningGrpcPort
    private int runningPort;

    @Value("#{environment.getProperty('local.grpc.port')}")
    Object localP;

    @Value("#{environment.getProperty('grpc.port')}")
    Object p;

    @Autowired
    GRpcServerProperties properties;


    @BeforeClass
    public static void startConfigServer() throws IOException, URISyntaxException {
        Properties properties = new Properties();
        properties.put("grpc.port","0");
        startConfigServer(properties);

    }




    @Test
    public void assertConfigServerConfiguredPort(){

        assertNotEquals(runningPort,properties.getPort().intValue());
        assertNotEquals(GRpcServerProperties.DEFAULT_GRPC_PORT,getPort());
        assertEquals(0,properties.getPort().intValue());
    }


}
