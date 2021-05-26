package org.lognet.springboot.grpc.auth;


import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import org.lognet.springboot.grpc.GrpcServerTestBase;
import org.lognet.springboot.grpc.security.AuthClientInterceptor;
import org.lognet.springboot.grpc.security.AuthHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;
import java.util.UUID;

public abstract class JwtAuthBaseTest extends GrpcServerTestBase {

    private boolean globalSecuredChannel = false;

    public JwtAuthBaseTest(boolean globalSecuredChannel) {
        this.globalSecuredChannel = globalSecuredChannel;
    }

    public JwtAuthBaseTest() {
        this.globalSecuredChannel = true;
    }

    @Value("${embedded.keycloak.auth-server-url:}")
    private String authServerUrl;

    @Override
    protected Channel getChannel() {
        return getChannel(globalSecuredChannel);

    }
    protected Channel getChannel(boolean authenticated){
            return authenticated ? ClientInterceptors.intercept(super.getChannel(), getAuthClientInterceptor())
                    : super.getChannel();

    }
    protected final static String USER_NAME = "keycloak-test";


    protected   AuthClientInterceptor getAuthClientInterceptor() {
        return new AuthClientInterceptor(
                AuthHeader.builder().bearer().tokenSupplier(this::generateToken));
    }

    protected ByteBuffer generateToken() {
        if (authServerUrl.isEmpty()) {
            return ByteBuffer.wrap(UUID.randomUUID().toString().getBytes());
        }

        final LinkedMultiValueMap<String, String> req = new LinkedMultiValueMap<>();


        req.add("client_id", "any-client");
        req.add("client_secret", "08f64721-7fef-4d8b-a0fc-8f940a621451");
        req.add("grant_type", "password");
        req.add("username", USER_NAME);
        req.add("password", "123Start!");


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(authServerUrl));
        final ResponseEntity<String> response = restTemplate
                .postForEntity("/realms/test-realm/protocol/openid-connect/token", new HttpEntity<>(req, headers), String.class);
        try {
            return ByteBuffer.wrap(new ObjectMapper().readTree(response.getBody())
                    .at("/access_token")
                    .asText().getBytes());
        } catch ( Exception e) {
            throw new RuntimeException(e);
        }
    }
}
