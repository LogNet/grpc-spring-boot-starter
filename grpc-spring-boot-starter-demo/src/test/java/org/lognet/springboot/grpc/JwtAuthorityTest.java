package org.lognet.springboot.grpc;


import io.grpc.*;
import io.grpc.examples.GreeterGrpc;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.security.EnableGrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.lognet.springboot.grpc.security.jwt.JwtAuthProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;


@SpringBootTest(classes = DemoApp.class)
@ActiveProfiles("keycloack-test")
@RunWith(SpringRunner.class)
@Import({JwtAuthorityTest.TestCfg.class})
public class JwtAuthorityTest extends GrpcServerTestBase {


    @Value("${embedded.keycloak.auth-server-url:}")
    private String authServerUrl;


    @TestConfiguration
    static class TestCfg {

        @EnableGrpcSecurity
        public class DemoGrpcSecurityConfig extends GrpcSecurityConfigurerAdapter {

            @Autowired
            private JwtDecoder jwtDecoder;

            @Override
            public void configure(GrpcSecurity builder) throws Exception {

                super.configure(builder);
                builder.authorizeRequests()
                        .methods(GreeterGrpc.getSayHelloMethod()).hasAnyAuthority("SCOPE_email")
                        .and()
                        .authenticationProvider(JwtAuthProviderFactory.withAuthorities(jwtDecoder));

            }


        }

    }



    @Override
    protected Channel getChannel() {
        String token = generateToken();

        return ClientInterceptors.intercept(super.getChannel(), new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel next) {
                return new ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT>(next.newCall(methodDescriptor, callOptions)) {
                    @Override
                    protected void checkedStart(Listener<RespT> responseListener, Metadata headers) throws Exception {
                        headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + token);
                        delegate().start(responseListener, headers);
                    }
                };
            }
        });
    }


    private String generateToken() {
        if (authServerUrl.isEmpty()) {
            return UUID.randomUUID().toString();
        }

        final LinkedMultiValueMap<String, String> req = new LinkedMultiValueMap<>();


        req.add("client_id", "any-client");
        req.add("client_secret", "08f64721-7fef-4d8b-a0fc-8f940a621451");
        req.add("grant_type", "password");
        req.add("username", "keycloak-test");
        req.add("password", "123Start!");


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(authServerUrl));
        final ResponseEntity<String> response = restTemplate
                .postForEntity("/realms/test-realm/protocol/openid-connect/token", new HttpEntity<>(req, headers), String.class);
        try {
            return new ObjectMapper().readTree(response.getBody())
                    .at("/access_token")
                    .asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
