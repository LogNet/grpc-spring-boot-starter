package org.lognet.grpc.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;



@SpringBootTest(classes = {DemoApp.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class DemoAppTest {
    @Test
    void contextLoaded() {

    }
}
