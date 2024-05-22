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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    static void beforeAll() {

        container.start();
    }

    @AfterAll
    static void afterAll() {
        container.stop();
    }
    @Test
    @SetEnvironmentVariable(key = KeycloakConfigurer.MASTER_REALM_ADMIN_USERNAME_ENV_VARIABLE, value = KEYCLOAK_ADMIN_USERNAME)
    @SetEnvironmentVariable(key = KeycloakConfigurer.MASTER_REALM_ADMIN_PASSWORD_ENV_VARIABLE, value = KEYCLOAK_ADMIN_PASSWORD)
    public void test() throws IOException {
        logger.debug("<test");

        {
            URL configUrl = getClass().getResource("/test-config");
            logger.debug("configUrl={}", configUrl);
            String keycloakUrl = container.getUrl();
            logger.debug("keycloakUrl={}", keycloakUrl);
            assertNotNull(configUrl);

            String[] args = {
                    String.format("--config=%s", configUrl.getPath()),
                    String.format("--keycloak-url=%s", keycloakUrl)
            };

            KeycloakConfigurer.main(args);
        }

        Pattern filterPattern = Pattern.compile("^DEBUG \\[org.keycloak.events\\].*$");
        Pattern resolvedLoggingEnabledPattern = Pattern.compile("^DEBUG \\[org.keycloak.events\\].*operationType=\"UPDATE\".*realm=\"test_resolved_eventLogger\".*resourceType=\"REALM\".*resourcePath=\"events/config\".*$");
        Pattern resolvedLoggerPattern = Pattern.compile("^DEBUG \\[org.keycloak.events\\].*operationType=\"([^\"]+)\".*realmId=\"([^\"]+)\".*realm=\"([^\"]+)\".*userId=\"([^\"]+)\".*userRealm=\"([^\"]+)\".*username=\"([^\"]+)\".*resourceType=\"([^\"]+)\".*resourcePath=\"([^\"]+)\".*$");

        List<String> auditLogs = keycloakLogs.stream().filter(log -> {
            logger.debug(log);
            logger.debug("pattern="+filterPattern.pattern());
            boolean matches = filterPattern.matcher(log).matches();
            logger.debug("matches="+matches);
            return matches;
        }).toList();
        logger.debug("auditLogs="+auditLogs.size());

        List<String> resolvedLoggingEnabledLogs = auditLogs.stream().filter(log -> resolvedLoggingEnabledPattern.matcher(log).matches()).toList();
        logger.debug("resolvedLoggingEnabledLogs="+resolvedLoggingEnabledLogs.size());

        assertFalse(resolvedLoggingEnabledLogs.isEmpty());
        String resolvedLoggingEnabledLog = resolvedLoggingEnabledLogs.get(0);

        int resolvedLoggingEnabledIndex = auditLogs.indexOf(resolvedLoggingEnabledLog);

        for (int i=resolvedLoggingEnabledIndex;i<auditLogs.size();++i) {
            String log = auditLogs.get(i);

            Matcher matcher = resolvedLoggerPattern.matcher(log);

            assertTrue(matcher.matches());

            String realm = matcher.group(3);
            assertEquals("test_resolved_eventLogger", realm);
            String userRealm = matcher.group(5);
            assertEquals("master", userRealm);
            String username = matcher.group(6);
            assertEquals(KEYCLOAK_ADMIN_USERNAME, username);
        }

        logger.debug(">test");
    }

}
