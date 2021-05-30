package org.lognet.springboot.grpc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ClassUtils;

import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE)
@ActiveProfiles("prefer-pure-netty")
@Import(DefaultNettyCollisionTest.TestConfig.class)
public class PreferPureNettyCollisionTest extends DefaultNettyCollisionTest {


    @Test
    public void contextLoads() {
      assertNettyBuilderClass(io.grpc.netty.NettyServerBuilder.class);

    }
}
