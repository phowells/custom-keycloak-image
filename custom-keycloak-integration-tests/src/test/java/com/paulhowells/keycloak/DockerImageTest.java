package com.paulhowells.keycloak;


import com.paulhowells.keycloak.configurer.KeycloakConfigurer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DockerImageTest {
    private static final Logger logger = LoggerFactory.getLogger(DockerImageTest.class);

    static final int KC_HTTP_PORT = 8080;
    static final String KEYCLOAK_ADMIN_USERNAME = "admin";
    static final String KEYCLOAK_ADMIN_PASSWORD = "admin";
    static final String KEYCLOAK_URL = "http://localhost:"+KC_HTTP_PORT;

    static final List<String> keycloakLogs = new ArrayList<>();

    static KeycloakContainer container = new KeycloakContainer()
        .withLogConsumer(outputFrame -> keycloakLogs.add(outputFrame.getUtf8String()));

    static {
        container.addEnv("KC_HTTP_PORT", String.valueOf(KC_HTTP_PORT));
        container.addEnv("KC_HEALTH_ENABLED", "true");
        container.addEnv("KC_METRICS_ENABLED", "true");
        container.addEnv("KC_LOG_LEVEL", "WARN,org.keycloak.events:DEBUG");
        container.addEnv("KEYCLOAK_ADMIN", KEYCLOAK_ADMIN_USERNAME);
        container.addEnv("KEYCLOAK_ADMIN_PASSWORD", KEYCLOAK_ADMIN_PASSWORD);
    }

    @BeforeAll
    static void beforeAll() {

        container.start();
    }

    @AfterAll
    static void afterAll() {
        container.stop();
    }
    @Test
    @SetEnvironmentVariable(key = KeycloakConfigurer.KEYCLOAK_URL_ENV_VARIABLE, value = KEYCLOAK_URL)
    @SetEnvironmentVariable(key = KeycloakConfigurer.MASTER_REALM_ADMIN_USERNAME_ENV_VARIABLE, value = KEYCLOAK_ADMIN_USERNAME)
    @SetEnvironmentVariable(key = KeycloakConfigurer.MASTER_REALM_ADMIN_PASSWORD_ENV_VARIABLE, value = KEYCLOAK_ADMIN_PASSWORD)
    public void test() throws IOException {
        logger.debug("<test");

        {
            URL configUrl = getClass().getResource("/keycloak-config");
            logger.debug("configUrl={}", configUrl);
            assertNotNull(configUrl);

            String[] args = {
                    String.format("--config=%s", configUrl.getPath())
            };

            KeycloakConfigurer.main(args);
        }

        keycloakLogs.forEach(logger::debug);

        logger.debug(">test");
    }

}
