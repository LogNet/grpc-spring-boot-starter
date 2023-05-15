package org.lognet.springboot.grpc.reactive;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.auth.JwtAuthBaseTest;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.rules.ExpectedStartupExceptionWithInspector;
import org.lognet.springboot.rules.SpringRunnerWithGlobalExpectedExceptionInspected;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.function.Predicate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@Slf4j
@RunWith(SpringRunnerWithGlobalExpectedExceptionInspected.class)
@SpringBootTest(classes = DemoApp.class)
@ActiveProfiles({"keycloack-test", "r2dbc-test", "reactive-buggy-security"})
@DirtiesContext
@ExpectedStartupExceptionWithInspector(BaggyReactiveSecurityTest.ExceptionInspector.class)
public class BaggyReactiveSecurityTest extends JwtAuthBaseTest {

    @Test
    public void contextStartupFails() {
    }

    public static class ExceptionInspector implements Predicate<Throwable> {

        @Override
        public boolean test(Throwable throwable) {

            Throwable rootCause = NestedExceptionUtils.getRootCause(throwable);
            assertThat(rootCause, instanceOf(BeanCreationException.class));
            BeanCreationException beanCreationException = (BeanCreationException) rootCause;

            assertThat(beanCreationException.getMessage(), allOf(
                    notNullValue(String.class),
                    stringContainsInOrder("Ambiguous", "Secured",  "method")
            ));

            return true;
        }

    }
}