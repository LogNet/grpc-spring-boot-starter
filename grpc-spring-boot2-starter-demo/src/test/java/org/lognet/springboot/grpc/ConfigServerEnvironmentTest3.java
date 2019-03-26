package org.lognet.springboot.grpc;

import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;


public class ConfigServerEnvironmentTest3 extends ConfigServerEnvironmentBaseTest{


    @Autowired
    GRpcServerProperties properties;


    @BeforeClass
    public static void startConfigServer() throws IOException, URISyntaxException {
        startConfigServer(new Properties());

    }




    @Test
    public void assertConfigServerConfiguredPort(){
        assertEquals(GRpcServerProperties.DEFAULT_GRPC_PORT,getPort());
        assertThat(properties.getPort(),CoreMatchers.nullValue(Integer.class));
    }


}
