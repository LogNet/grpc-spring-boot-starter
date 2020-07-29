package org.lognet.springboot.grpc.demo;



import org.lognet.springboot.grpc.security.GrpcSecurityConfiguration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.AbstractEnvironment;

/**
 * Created by alexf on 28-Jan-16.
 */
@SpringBootApplication
public class DemoApp {
    public static void main(String[] args) {
        System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME,"app");
        final ConfigurableApplicationContext applicationContext = SpringApplication.run(DemoApp.class, args);

    }

}
