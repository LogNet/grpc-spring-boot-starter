package org.lognet.springboot.grpc.simple;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Created by alexf on 28-Jan-16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = RANDOM_PORT
        , properties = {
        "management.endpoints.web.exposure.include=*"
        , "spring.main.web-application-type=servlet"
})
@ActiveProfiles({"disable-security"})
public class Issue295Test extends GrpcServerTestBase {


    @Autowired
    private TestRestTemplate restTemplate;


    @Test
    public void configPropertiesTest() throws ExecutionException, InterruptedException {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/configprops", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

}
