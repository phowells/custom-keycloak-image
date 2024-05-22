package com.paulhowells.keycloak;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DockerImageTest {
    private static final Logger logger = LoggerFactory.getLogger(DockerImageTest.class);

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
    static void beforeAll() {
        container.start();
    }

    @AfterAll
    static void afterAll() {
        container.stop();
    }

    @Test
    public void test() {
        logger.debug("<test");

        logger.debug(">test "+keycloakLogs.size());
    }

}
