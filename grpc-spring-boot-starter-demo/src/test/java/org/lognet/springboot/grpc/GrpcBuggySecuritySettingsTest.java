package org.lognet.springboot.grpc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.rules.ExpectedStartupExceptionWithInspector;
import org.lognet.springboot.rules.SpringRunnerWithGlobalExpectedExceptionInspected;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.function.Predicate;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;


@RunWith(SpringRunnerWithGlobalExpectedExceptionInspected.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE)
@ActiveProfiles("buggy-security")
@ExpectedStartupExceptionWithInspector(GrpcBuggySecuritySettingsTest.ExceptionInspector.class)
public class GrpcBuggySecuritySettingsTest extends GrpcServerTestBase {
    @Test
    public void contextFails() {
    }

    public static class ExceptionInspector implements Predicate<Throwable> {

        @Override
        public boolean test(Throwable throwable) {
            Throwable rootCause = throwable;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            return rootCause instanceof BeanCreationException;
        }

    }

}
