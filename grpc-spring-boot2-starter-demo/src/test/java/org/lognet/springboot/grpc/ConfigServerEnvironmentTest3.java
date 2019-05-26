package org.lognet.springboot.grpc;

import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

@DirtiesContext
public class ConfigServerEnvironmentTest3 extends ConfigServerEnvironmentBaseTest{

    @BeforeClass
    public static void startConfigServer() throws IOException, URISyntaxException {
        startConfigServer(new Properties());

    }

    @Test
    public void assertConfigServerConfiguredPort(){
        assertEquals(GRpcServerProperties.DEFAULT_GRPC_PORT,getPort());
        assertThat(gRpcServerProperties.getPort(),CoreMatchers.nullValue(Integer.class));
    }


}
