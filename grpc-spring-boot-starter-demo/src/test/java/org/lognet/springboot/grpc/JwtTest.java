package org.lognet.springboot.grpc;


import io.grpc.*;
import net.minidev.json.JSONNavi;
import net.minidev.json.JSONObject;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.lognet.springboot.grpc.security.EnableGrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurity;
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@SpringBootTest(classes = DemoApp.class)
@ActiveProfiles("keycloack-test")
@RunWith(SpringRunner.class)
@Import({JwtTest.TestCfg.class})
public class JwtTest extends GrpcServerTestBase {


    @Value("${embedded.keycloak.auth-server-url:}")
    private String authServerUrl;


    @TestConfiguration
    static class TestCfg {

        @EnableGrpcSecurity
        public class DemoGrpcSecurityConfig extends GrpcSecurityConfigurerAdapter {

            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
            private String issuerUri;

            @Override
            public void configure(GrpcSecurity builder) throws Exception {

                super.configure(builder);
                builder.authorizeRequests().anyMethod().hasRole("ROLE_reader")
                        .and()
                        .authenticationProvider(customJwt());

            }

            private AuthenticationProvider customJwt() {
                final JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
                authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
                    JSONObject resourceAccess = jwt.getClaim("resource_access");
                    final JSONNavi<?> roles = JSONNavi.newInstanceArray()
                            .add(resourceAccess)
                            .at(0)
                            .at(jwt.getClaimAsString("azp"))
                            .at("roles");

                    return IntStream.range(0, roles.getSize())
                            .mapToObj(k ->  new SimpleGrantedAuthority("ROLE_" + roles.get(k).toString()))
                            .collect(Collectors.toList());

                });
                final JwtAuthenticationProvider authenticationProvider = new JwtAuthenticationProvider(JwtDecoders.fromOidcIssuerLocation(issuerUri));
                authenticationProvider.setJwtAuthenticationConverter(authenticationConverter);
                return authenticationProvider;
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
