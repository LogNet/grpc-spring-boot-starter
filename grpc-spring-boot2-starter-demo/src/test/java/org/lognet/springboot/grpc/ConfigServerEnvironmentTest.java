package org.lognet.springboot.grpc;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DemoApp.class,
        // Normally spring.cloud.config.enabled:true is the default but since we have the
        // config server on the classpath we need to set it explicitly
        properties = { "spring.cloud.config.enabled:true"})
public class ConfigServerEnvironmentTest extends GrpcServerTestBase{

    private static int configPort = SocketUtils.findAvailableTcpPort();
    private static ConfigurableApplicationContext server;

    @BeforeClass
    public static void startConfigServer() throws IOException, URISyntaxException {
        File configDir = Paths.get(ConfigServerEnvironmentTest.class.getResource("/config-repo/grpc-demo.properties").toURI())
                .toFile()
                .getParentFile();




        server = SpringApplication.run(org.springframework.cloud.config.server.ConfigServerApplication.class,
                "--server.port=" + configPort,
                "--spring.autoconfigure.exclude=org.lognet.springboot.grpc.autoconfigure.GRpcAutoConfiguration",
                "--spring.cloud.config.server.health.enabled=false",
                "--spring.cloud.config.server.bootstrap=false",
                "--spring.profiles.active=native",
                "--spring.cloud.config.server.native.search-locations[0]=file:"+configDir.getAbsolutePath()
        );
        System.setProperty("config.port", "" + configPort);

    }

    @Test
    public void assertConfigServerConfiguredPort(){
        assertEquals(6666,getPort());
    }
    @AfterClass
    public static void close() {
        System.clearProperty("config.port");
        Optional.ofNullable(server).ifPresent(ConfigurableApplicationContext::close);
    }

}
