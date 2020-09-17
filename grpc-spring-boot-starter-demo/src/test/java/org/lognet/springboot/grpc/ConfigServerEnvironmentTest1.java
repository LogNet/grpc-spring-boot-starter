package org.lognet.springboot.grpc;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;
@DirtiesContext
public class ConfigServerEnvironmentTest1 extends ConfigServerEnvironmentBaseTest{



    @BeforeClass
    public static void startConfigServer( ) throws IOException, URISyntaxException {
        Properties properties = new Properties();
        properties.put("grpc.port","6666");
        properties.put("grpc.shutdownGrace","1");
        startConfigServer(properties);

    }





    @Test
    public void assertConfigServerConfiguredPort(){
        Assert.assertEquals(6666,getPort());
    }


}
