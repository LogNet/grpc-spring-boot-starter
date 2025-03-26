package org.lognet.springboot.grpc;

import io.grpc.Attributes;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.autoconfigure.metrics.RequestAwareGRpcMetricsTagsContributor;
import org.lognet.springboot.grpc.context.LocalRunningGrpcPort;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE, properties = {"grpc.port=0"})
@ActiveProfiles("measure")
@Import(GrpcMeterTest.Config.class)
public class GrpcMeterTest extends GrpcServerTestBase {
    @TestConfiguration
    static class Config{
        @Bean
        public RequestAwareGRpcMetricsTagsContributor<GreeterOuterClass.HelloRequest> helloContributor(){
            return new RequestAwareGRpcMetricsTagsContributor<GreeterOuterClass.HelloRequest>(GreeterOuterClass.HelloRequest.class) {
                @Override
                public Iterable<Tag> addTags(GreeterOuterClass.HelloRequest request, MethodDescriptor<?, ?> methodDescriptor, Attributes attributes, Tags tags) {
                    return Collections.singletonList(Tag.of("hello",request.getName()));
                }

                @Override
                public Iterable<Tag> getTags(Status status, MethodDescriptor<?, ?> methodDescriptor, Attributes attributes) {
                    return Collections.singletonList(Tag.of("customTagName",status.getCode().name()));
                }
            };
        }
        @Bean
        public RequestAwareGRpcMetricsTagsContributor<GreeterOuterClass.Person> shouldNotBeInvoked(){
            return new RequestAwareGRpcMetricsTagsContributor<GreeterOuterClass.Person>(GreeterOuterClass.Person.class) {
                @Override
                public Iterable<Tag> addTags(GreeterOuterClass.Person request, MethodDescriptor<?, ?> methodDescriptor, Attributes attributes, Tags tags) {
                    return Collections.emptyList();
                }

                @Override
                public Iterable<Tag> getTags(Status status, MethodDescriptor<?, ?> methodDescriptor, Attributes attributes) {
                    return Collections.emptyList();
                }
            };
        }

        @Bean
        public RequestAwareGRpcMetricsTagsContributor<GreeterOuterClass.HelloRequest> multiHelloContributor() {
            return new RequestAwareGRpcMetricsTagsContributor<GreeterOuterClass.HelloRequest>(GreeterOuterClass.HelloRequest.class, BIDI_STREAMING) {
                @Override
                public Tags addTags(GreeterOuterClass.HelloRequest request, MethodDescriptor<?, ?> methodDescriptor, Attributes attributes, Tags existingTags) {
                    String existingTag = existingTags.stream()
                        .filter(tag -> tag.getKey().equals("many-hellos"))
                        .findAny()
                        .map(Tag::getValue)
                        .orElse("");
                    return Tags.of("many-hellos", existingTag.isEmpty() ? request.getName() : existingTag + ", " + request.getName());
                }

                @Override
                public Iterable<Tag> getTags(Status status, MethodDescriptor<?, ?> methodDescriptor, Attributes attributes) {
                    return Collections.singletonList(Tag.of("endTag", status.getCode().name()));
                }
            };
        }
    }

    @SpyBean
    private RequestAwareGRpcMetricsTagsContributor<GreeterOuterClass.Person> shouldNotBeInvoked;

    @Autowired
    private MeterRegistry registry;

    @LocalRunningGrpcPort
    private int port;

    @Autowired
    private PrometheusConfig registryConfig;

    @Before
    public void setUp()  {
        registry.clear();
    }

    @Override
    protected void afterGreeting()  {


        final Timer timer = registry.find("grpc.server.calls").timer();
        assertThat(timer,notNullValue(Timer.class));

        Awaitility
                .waitAtMost(Duration.ofMillis(registryConfig.step().toMillis() * 2))
                .until(timer::count,greaterThan(0L));

        assertThat(timer.max(TimeUnit.MILLISECONDS),greaterThan(0d));
        assertThat(timer.mean(TimeUnit.MILLISECONDS),greaterThan(0d));
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS),greaterThan(0d));


        final String addressTag = timer.getId().getTag("address");
        assertThat(addressTag,notNullValue());
        assertThat(addressTag,containsString(String.valueOf(port)));

        final String methodTag = timer.getId().getTag("method");
        assertThat(methodTag,notNullValue());
        assertThat(methodTag,is(GreeterGrpc.getSayHelloMethod().getFullMethodName()));

        final String resultTag = timer.getId().getTag("result");
        assertThat(resultTag,notNullValue());
        assertThat(resultTag,is(Status.OK.getCode().name()));

        //from contributor

        final String helloTag = timer.getId().getTag("hello");
        assertThat(helloTag,notNullValue());
        assertThat(helloTag,is(name));

        final String customTag = timer.getId().getTag("customTagName");
        assertThat(customTag,notNullValue());
        assertThat(customTag,is(Status.OK.getCode().name()));

        Mockito.verify(shouldNotBeInvoked,Mockito.times(1)).getTags(Mockito.any(Status.class),Mockito.any(),Mockito.any());
        Mockito.verify(shouldNotBeInvoked,Mockito.never()).getTags(Mockito.any(GreeterOuterClass.Person.class),Mockito.any(),Mockito.any());
        Mockito.verify(shouldNotBeInvoked, Mockito.never())
            .addTags(Mockito.any(GreeterOuterClass.Person.class), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void tagsForStream() {
        final GreeterGrpc.GreeterStub greeterFutureStub = GreeterGrpc.newStub(selectedChanel);
        io.grpc.stub.StreamObserver<GreeterOuterClass.HelloRequest> helloInput =
            greeterFutureStub.sayManyHellos(new StreamObserver<GreeterOuterClass.HelloReply>() {
                @Override
                public void onNext(GreeterOuterClass.HelloReply value) {}

                @Override
                public void onError(Throwable t) {}

                @Override
                public void onCompleted() {}
            });
        Stream.of("a", "b", "c", "d")
            .map(name -> GreeterOuterClass.HelloRequest.newBuilder().setName(name).build())
            .forEach(helloInput::onNext);
        helloInput.onCompleted();

        final Timer timer = Awaitility
            .waitAtMost(Duration.ofMillis(registryConfig.step().toMillis() * 2))
            .ignoreExceptionsInstanceOf(MeterNotFoundException.class)
            .until(
                () -> registry.get("grpc.server.calls")
                    .tags("method", GreeterGrpc.getSayManyHellosMethod().getFullMethodName())
                    .timer(),
                Objects::nonNull
            );

        assertThat(timer.totalTime(TimeUnit.MILLISECONDS), greaterThan(0d));
        assertThat(timer.getId().getTag("many-hellos"), is("a, b, c, d"));
        assertThat(timer.getId().getTag("endTag"), is(Status.OK.getCode().name()));
    }
}
