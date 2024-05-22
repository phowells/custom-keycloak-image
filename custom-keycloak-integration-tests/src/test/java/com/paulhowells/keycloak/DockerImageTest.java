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

import static org.junit.jupiter.api.Assertions.*;

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
    static void beforeAll() throws IOException {

        container.start();

        URL configUrl = DockerImageTest.class.getResource("/test-config");
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

    @AfterAll
    static void afterAll() {
        container.stop();
    }

    @Test
    public void testResolvedEventLogging() throws IOException {
        logger.debug("<testResolvedEventLogging");

        List<String> auditLogs = keycloakLogs.stream().filter(log -> log.contains("DEBUG [org.keycloak.events]")).toList();
        assertFalse(auditLogs.isEmpty());

        List<String> resolvedLoggingEnabledLogs = auditLogs.stream().filter(log -> log.contains("operationType=\"UPDATE\"") && log.contains("resourceType=\"REALM\"") && log.contains("resourcePath=\"events/config\"")).toList();

        assertFalse(resolvedLoggingEnabledLogs.isEmpty());
        String resolvedLoggingEnabledLog = resolvedLoggingEnabledLogs.get(0);

        int resolvedLoggingEnabledIndex = auditLogs.indexOf(resolvedLoggingEnabledLog);
        assertTrue(resolvedLoggingEnabledLogs.size() > resolvedLoggingEnabledIndex + 1);

        for (int i=resolvedLoggingEnabledIndex + 1;i<auditLogs.size();++i) {
            String log = auditLogs.get(i);

            logger.debug("KEYCLOAK LOG: "+log);
            assertTrue(log.contains("realm=\"test_resolved_eventLogger\""));
            assertTrue(log.contains("userRealm=\"master\""));
            assertTrue(log.contains("username=\""+KEYCLOAK_ADMIN_USERNAME+"\""));
        }

        logger.debug(">testResolvedEventLogging");
    }

}
