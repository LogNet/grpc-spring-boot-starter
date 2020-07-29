package org.lognet.springboot.grpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

@RunWith(SpringRunner.class)
public abstract class GrpcServerJUnit5TestBase extends GrpcServerTestBase{


    @BeforeEach
    @Override
    public void setupChannels() throws IOException {
        super.setupChannels();
    }

    @AfterEach
    @Override
    public void shutdownChannels() {
        super.shutdownChannels();
    }

    @Override
    public void simpleGreeting() throws ExecutionException, InterruptedException {
        //super.simpleGreeting();
    }

    @Test
    public void simpleGreetingJUnit5() throws ExecutionException, InterruptedException {
        super.simpleGreeting();
    }
}
