package org.lognet.springboot.grpc;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Properties;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = DemoApp.class,
        // Normally spring.cloud.config.enabled:true is the default but since we have the
        // config server on the classpath we need to set it explicitly
        properties = { "spring.cloud.config.enabled:true"})
public abstract class ConfigServerEnvironmentBaseTest extends GrpcServerTestBase{

    private static int configPort = SocketUtils.findAvailableTcpPort();
    private static ConfigurableApplicationContext server;

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    protected Environment environment;


    public static void startConfigServer(Properties properties) throws IOException, URISyntaxException {

        File cfgFile = temporaryFolder.newFile("grpc-demo.properties");
        try(OutputStream os = new FileOutputStream(cfgFile)) {
            properties.store(os,null);
        }


        server = SpringApplication.run(org.springframework.cloud.config.server.ConfigServerApplication.class,
                "--server.port=" + configPort,
                "--spring.autoconfigure.exclude=org.lognet.springboot.grpc.autoconfigure.GRpcAutoConfiguration",
                "--spring.cloud.config.server.health.enabled=false",
                "--spring.cloud.config.server.bootstrap=false",
                "--spring.profiles.active=native",
                "--grpc.enabled=false",
                "--spring.cloud.config.server.native.search-locations[0]=file:"+temporaryFolder.getRoot().getAbsolutePath()
        );
        System.setProperty("config.port", "" + configPort);

    }



    @AfterClass
    public static void close() {
        System.clearProperty("config.port");
        Optional.ofNullable(server).ifPresent(ConfigurableApplicationContext::close);
    }

}
