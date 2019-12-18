package org.lognet.springboot.grpc;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
@DirtiesContext
public class ConfigServerEnvironmentTest1 extends ConfigServerEnvironmentBaseTest{


    @BeforeClass
    public static void startConfigServer() throws IOException, URISyntaxException {
        Properties properties = new Properties();
        properties.put("grpc.port","6666");
        properties.put("grpc.shutdownGrace","1");
        startConfigServer(properties);

    }
    @AfterClass
    public static void close() {
        System.clearProperty("grpc.port");
    }




    @Test
    public void assertConfigServerConfiguredPort(){
        assertEquals(6666,getPort());
    }


}
