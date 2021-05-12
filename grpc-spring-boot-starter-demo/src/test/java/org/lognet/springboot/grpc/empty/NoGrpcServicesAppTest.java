package org.lognet.springboot.grpc.empty;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EmptyDemoApp.class})
public class NoGrpcServicesAppTest {
    @Test
    public void contextLoads() {

    }
}
