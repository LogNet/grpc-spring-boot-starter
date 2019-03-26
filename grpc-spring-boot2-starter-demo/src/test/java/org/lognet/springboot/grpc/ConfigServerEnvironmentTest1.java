package org.lognet.springboot.grpc;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;


public class ConfigServerEnvironmentTest1 extends ConfigServerEnvironmentBaseTest{


    @BeforeClass
    public static void startConfigServer() throws IOException, URISyntaxException {
        Properties properties = new Properties();
        properties.put("grpc.port","6666");
        startConfigServer(properties);

    }




    @Test
    public void assertConfigServerConfiguredPort(){
        assertEquals(6666,getPort());
    }


}
