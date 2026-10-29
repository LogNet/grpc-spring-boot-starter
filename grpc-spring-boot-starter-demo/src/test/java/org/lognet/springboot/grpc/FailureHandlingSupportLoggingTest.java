package org.lognet.springboot.grpc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lognet.springboot.grpc.recovery.GRpcExceptionHandlerMethodResolver;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

public class FailureHandlingSupportLoggingTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(FailureHandlingSupport.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @Before
    public void attachAppender() {
        appender.start();
        logger.addAppender(appender);
    }

    @After
    public void detachAppender() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    public void unhandledExceptionIsLoggedWithItsStackTrace() {
        final GRpcExceptionHandlerMethodResolver resolver = Mockito.mock(GRpcExceptionHandlerMethodResolver.class);
        Mockito.when(resolver.resolveMethodByThrowable(anyString(), any())).thenReturn(Optional.empty());

        final MethodDescriptor.Marshaller<String> marshaller = Mockito.mock(MethodDescriptor.Marshaller.class);
        final MethodDescriptor<String, String> descriptor = MethodDescriptor.<String, String>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName("svc/method")
                .setRequestMarshaller(marshaller)
                .setResponseMarshaller(marshaller)
                .build();
        final ServerCall<?, ?> call = Mockito.mock(ServerCall.class);
        Mockito.when(call.getMethodDescriptor()).thenAnswer(i -> descriptor);

        final IllegalStateException failure = new IllegalStateException("boom");

        new FailureHandlingSupport(resolver).closeCall(failure, call, new Metadata());

        assertThat(appender.list, hasSize(1));
        final ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel(), is(Level.WARN));
        assertThat(event.getThrowableProxy(), notNullValue());
        assertThat(event.getThrowableProxy().getMessage(), is("boom"));
    }
}
