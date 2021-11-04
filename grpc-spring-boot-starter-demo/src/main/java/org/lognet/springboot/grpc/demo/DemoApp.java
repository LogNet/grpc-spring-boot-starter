package org.lognet.springboot.grpc.demo;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Created by alexf on 28-Jan-16.
 */
@SpringBootApplication
public class DemoApp {

    public static void main(String[] args) {
        new SpringApplicationBuilder(DemoApp.class)
                .profiles("demo")
                .run(args);
    }

}
