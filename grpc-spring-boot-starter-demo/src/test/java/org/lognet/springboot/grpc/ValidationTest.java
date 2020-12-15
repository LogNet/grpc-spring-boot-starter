package org.lognet.springboot.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.junit.Assert.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE, properties = {"grpc.port=0"})
public class ValidationTest extends GrpcServerTestBase {
    private  GreeterGrpc.GreeterBlockingStub stub;



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


    }


    String getFieldName(int fieldNumber) {
        return GreeterOuterClass.Person.getDescriptor().findFieldByNumber(fieldNumber).getName();
    }
}
