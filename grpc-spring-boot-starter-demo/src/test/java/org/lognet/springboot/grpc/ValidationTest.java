package org.lognet.springboot.grpc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.junit.Assert.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.util.Locale;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE, properties = {"grpc.port=0"})
@Import(ValidationTest.TestCfg.class)
@ActiveProfiles("disable-security")
public class ValidationTest extends GrpcServerTestBase {
    @TestConfiguration
    static class TestCfg {
        @Bean
        public GRpcErrorHandler authErrorHandler() {
            return new GRpcErrorHandler() {
                @Override
                public Status handle(Object message, Status status, Exception exception, Metadata requestHeaders, Metadata responseHeaders) {
                    responseHeaders.put(Metadata.Key.of("test", Metadata.ASCII_STRING_MARSHALLER), "val");
                    return super.handle(message,status,exception,requestHeaders,responseHeaders);
                }
            };
        }
    }
    private  GreeterGrpc.GreeterBlockingStub stub;

    private static Locale systemDefaultLocale;

    @BeforeClass
    public static void setLocaleToEnglish() {
        systemDefaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
    }

    @AfterClass
    public static void resetDefaultLocale() {
        Locale.setDefault(systemDefaultLocale);
    }

    @Before
    public void setUp() throws Exception {
        stub = GreeterGrpc.newBlockingStub(getChannel());
    }

    @Test
    public void stringValidationTest() {


        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> {
            stub.helloPersonValidResponse(GreeterOuterClass.Person.newBuilder()
                    .setAge(49)// valid
                    .clearName()//invalid
                    .build());
        });
        assertThat(e.getStatus().getCode(), Matchers.is(Status.Code.INVALID_ARGUMENT));
        assertThat(e.getMessage(), Matchers.allOf(
                Matchers.containsStringIgnoringCase("must not be empty"),
                Matchers.containsStringIgnoringCase(getFieldName(GreeterOuterClass.Person.NAME_FIELD_NUMBER))

        ));

    }

    @Test
    public void numberValidationTest() {

        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> {
            stub.helloPersonValidResponse(GreeterOuterClass.Person.newBuilder()
                    .setAge(121)//invalid
                    .setName("Dexter")//valid
                    .build());
        });
        assertThat(e.getStatus().getCode(), Matchers.is(Status.Code.INVALID_ARGUMENT));
        assertThat(e.getMessage(), Matchers.allOf(
                Matchers.containsStringIgnoringCase("must be less than or equal to"),
                Matchers.containsStringIgnoringCase(getFieldName(GreeterOuterClass.Person.AGE_FIELD_NUMBER))
        ));
    }

    @Test
    public void fieldsValidationTest() {

        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> {
            stub.helloPersonValidResponse(GreeterOuterClass.Person.newBuilder()
                    .setAge(121)//invalid
                    .clearName()//invalid
                    .build());
        });
        assertThat(e.getStatus().getCode(), Matchers.is(Status.Code.INVALID_ARGUMENT));
        assertThat(e.getMessage(), Matchers.allOf(
                Matchers.containsStringIgnoringCase(getFieldName(GreeterOuterClass.Person.AGE_FIELD_NUMBER)),
                Matchers.containsStringIgnoringCase("must be less than or equal to"),

                Matchers.containsStringIgnoringCase(getFieldName(GreeterOuterClass.Person.NAME_FIELD_NUMBER)),
                Matchers.containsStringIgnoringCase("must not be empty"),

                Matchers.containsStringIgnoringCase(getFieldName(GreeterOuterClass.Person.ADDRESS_FIELD_NUMBER)),
                Matchers.containsStringIgnoringCase("must be true")

        ));
        assertResponseHeaders(e);
    }

    @Test
    public void crossFieldsValidationTest() {

        final GreeterOuterClass.Person person = GreeterOuterClass.Person.newBuilder()
                .setAge(4)//valid
                .setName("Bob")//valid
                .build();
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> {

            stub.helloPersonValidResponse(person);
        });
        // Person constraint should fail
        assertThat(e.getStatus().getCode(), Matchers.is(Status.Code.INVALID_ARGUMENT));
        assertThat(e.getMessage(),Matchers.allOf(
                Matchers.containsStringIgnoringCase("Person with name"),
                Matchers.containsStringIgnoringCase("can't be of age"),
                Matchers.containsStringIgnoringCase(person.getName()),
                Matchers.containsStringIgnoringCase(Integer.toString(person.getAge()))
        ));
        assertResponseHeaders(e);



    }
    @Test
    public void validMessageValidationTest() {
        //valid
        final GreeterOuterClass.Person response = stub.helloPersonValidResponse(GreeterOuterClass.Person.newBuilder()
                .setAge(3)//valid
                .setName("Dexter")//valid
                .setAddress(GreeterOuterClass.Address.newBuilder())
                .build());
        assertThat(response.getNickName(),Matchers.not(emptyOrNullString()));
    }

    @Test
    public void invalidResponseMessageValidationTest() {
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> {

            stub.helloPersonInvalidResponse(GreeterOuterClass.Person.newBuilder()
                    .setAge(3)//valid
                    .setName("Dexter")//valid
                    .setAddress(GreeterOuterClass.Address.newBuilder())
                    .build());
        });
        assertThat(e.getStatus().getCode(), Matchers.is(Status.Code.FAILED_PRECONDITION));
        assertThat(e.getMessage(), Matchers.allOf(

                Matchers.containsStringIgnoringCase(getFieldName(GreeterOuterClass.Person.NICKNAME_FIELD_NUMBER)),
                Matchers.containsStringIgnoringCase("must not be empty")
        ));
        assertResponseHeaders(e);

    }


    String getFieldName(int fieldNumber) {
        return GreeterOuterClass.Person.getDescriptor().findFieldByNumber(fieldNumber).getName();
    }

    private void assertResponseHeaders(StatusRuntimeException e) {
        final String header = e.getTrailers().get(Metadata.Key.of("test", Metadata.ASCII_STRING_MARSHALLER));
        assertThat(header, Matchers.is("val"));
    }
}
