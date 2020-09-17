package org.lognet.springboot.grpc;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

@DirtiesContext
public class ConfigServerEnvironmentTest3 extends ConfigServerEnvironmentBaseTest{



    @BeforeClass
    public static void startConfigServer() throws IOException, URISyntaxException {
        startConfigServer(new Properties());

    }

    @Test
    public void assertConfigServerConfiguredPort(){
        Assert.assertEquals(GRpcServerProperties.DEFAULT_GRPC_PORT,getPort());
        MatcherAssert.assertThat(gRpcServerProperties.getPort(),CoreMatchers.nullValue(Integer.class));
    }


}
