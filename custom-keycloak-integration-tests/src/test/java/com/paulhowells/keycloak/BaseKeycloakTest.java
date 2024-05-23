package com.paulhowells.keycloak;


import com.paulhowells.keycloak.configurer.KeycloakConfigurer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class BaseKeycloakTest {
    private static final Logger logger = LoggerFactory.getLogger(BaseKeycloakTest.class);
    static final String KEYCLOAK_ADMIN_USERNAME = "admin";
    static final String KEYCLOAK_ADMIN_PASSWORD = "admin";

    static final List<String> keycloakLogs = new ArrayList<>();

    static KeycloakContainer container = new KeycloakContainer()
        .withLogConsumer(outputFrame -> keycloakLogs.add(outputFrame.getUtf8String()));

    static {
        container.addEnv("KC_HEALTH_ENABLED", "true");
        container.addEnv("KC_METRICS_ENABLED", "true");
        container.addEnv("KC_LOG_LEVEL", "WARN,org.keycloak.events:DEBUG");
        container.addEnv("KEYCLOAK_ADMIN", KEYCLOAK_ADMIN_USERNAME);
        container.addEnv("KEYCLOAK_ADMIN_PASSWORD", KEYCLOAK_ADMIN_PASSWORD);
    }

    @BeforeAll
    static void beforeAll() throws IOException {

        // We only want to start the Keycloak container once for all the subclasses.
        // This will speed up the tests significantly
        // We don't need to explicitly stop the container
        // Ryuk will monitor and terminate the containers on JVM exit
        if (!container.isRunning()) {

            keycloakLogs.clear();
            container.start();

            URL configUrl = BaseKeycloakTest.class.getResource("/test-config");
            logger.debug("configUrl={}", configUrl);
            String keycloakUrl = container.getUrl();
            logger.debug("keycloakUrl={}", keycloakUrl);
            assertNotNull(configUrl);

            String[] args = {
                    String.format("--config=%s", configUrl.getPath()),
                    String.format("--keycloak-url=%s", keycloakUrl),
                    String.format("--username=%s", KEYCLOAK_ADMIN_USERNAME),
                    String.format("--password=%s", KEYCLOAK_ADMIN_PASSWORD)
            };

            KeycloakConfigurer.main(args);
        }
    }
}
