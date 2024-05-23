package com.paulhowells.keycloak;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OtherDockerImageTest extends BaseKeycloakTest {
    private static final Logger logger = LoggerFactory.getLogger(OtherDockerImageTest.class);

    @Test
    public void testOtherResolvedEventLogging() throws IOException {
        logger.debug("<testOtherResolvedEventLogging");

        assertFalse(keycloakLogs.isEmpty(), "Expecting to find some Keycloak logs");
        keycloakLogs.forEach(log -> logger.debug("KEYCLOAK LOG: "+log));

        List<String> auditLogs = keycloakLogs.stream().filter(log -> log.contains("DEBUG [org.keycloak.events]")).toList();
        assertFalse(auditLogs.isEmpty(), "Expecting to find some audit logs");

        List<String> resolvedLoggingEnabledLogs = auditLogs.stream().filter(log -> log.contains("operationType=\"UPDATE\"") && log.contains("resourceType=\"REALM\"") && log.contains("resourcePath=\"events/config\"")).toList();

        assertFalse(resolvedLoggingEnabledLogs.isEmpty(), "Expecting to find a log entry for the enabling of the resolved logging provider");
        String resolvedLoggingEnabledLog = resolvedLoggingEnabledLogs.get(0);

        int resolvedLoggingEnabledIndex = auditLogs.indexOf(resolvedLoggingEnabledLog);
        assertTrue(auditLogs.size() > resolvedLoggingEnabledIndex + 1);

        for (int i=resolvedLoggingEnabledIndex + 1;i<auditLogs.size();++i) {
            String log = auditLogs.get(i);

            logger.debug("AUDIT LOG: "+log);
            assertTrue(log.contains("realm=\"test_resolved_eventLogger\""), "Expecting log entry to show the test_resolved_eventLogger realm.");
            assertTrue(log.contains("userRealm=\"master\""), "Expecting log entry to show the master user realm.");
            assertTrue(log.contains("username=\""+KEYCLOAK_ADMIN_USERNAME+"\""), "Expecting log entry to show the "+KEYCLOAK_ADMIN_USERNAME+" user.");
        }

        logger.debug(">testOtherResolvedEventLogging");
    }

}
