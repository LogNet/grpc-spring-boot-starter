package org.lognet.springboot.grpc.auth;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

public class KeycloakContainerInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final KeycloakContainer KEYCLOAK;

    static {
        KEYCLOAK = new KeycloakContainer()
                .waitingFor(Wait.forHttp("/realms/master")
                        .forPort(8080)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(3)))
                .withStartupTimeout(Duration.ofMinutes(3))
                .withRealmImportFile("test-realm-realm.json");
        KEYCLOAK.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String authServerUrl = KEYCLOAK.getAuthServerUrl();
        if (!authServerUrl.endsWith("/")) {
            authServerUrl = authServerUrl + "/";
        }
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                "embedded.keycloak.auth-server-url=" + authServerUrl,
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=" + authServerUrl + "realms/test-realm"
        );
    }

    public static KeycloakContainer getContainer() {
        return KEYCLOAK;
    }
}
