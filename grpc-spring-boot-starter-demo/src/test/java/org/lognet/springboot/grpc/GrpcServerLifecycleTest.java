package org.lognet.springboot.grpc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.context.GRpcServerInitializedEvent;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE)
@Import(GrpcServerLifecycleTest.Config.class)
@ActiveProfiles("disable-security")
public class GrpcServerLifecycleTest extends GrpcServerTestBase {
    @TestConfiguration
    public static  class Config{
        @Bean
        public CommandLineRunner testCommandLineRunner(GRpcServerRunner runner,@Qualifier("serverStartedBeforeRunner") AtomicBoolean serverStartedBeforeRunner){
            return  new CommandLineRunner() {
                @Override
                public void run(String... args) throws Exception {
                      serverStartedBeforeRunner.set(runner.isRunning());
                }
            };
        }
        @EventListener
        public  void onServerStartListener(GRpcServerInitializedEvent e){
            eventFired().set(true);
        }

        @Bean
        public AtomicBoolean eventFired(){
            return  new AtomicBoolean(false);
        }

        @Bean
        public AtomicBoolean serverStartedBeforeRunner(){
            return  new AtomicBoolean(false);
        }


    }
    @Qualifier("serverStartedBeforeRunner")
    @Autowired
    private AtomicBoolean serverStartedBeforeRunner;

    @Qualifier("eventFired")
    @Autowired
    private AtomicBoolean eventFired;

    @Test
    public void contextStarter() {
        assertThat("Event should be fired",eventFired.get());
        assertThat("Server should be started before command line runner",serverStartedBeforeRunner.get());
    }
}
