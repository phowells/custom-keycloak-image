package com.paulhowells.keycloak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class AbstractTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTest.class);

    protected static final String keycloakUrl = "https://keycloak.paulhowells.dev";
    protected static final String keycloakMasterRealmName = "master";
    protected static final String masterAdminUsername = "admin";
    protected static final String masterAdminPassword = "admin";

    protected Map<String, Object> createRealm(String realmName) throws IOException {

        Map<String, Object> result;

        try(KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                keycloakUrl,
                keycloakMasterRealmName,
                masterAdminUsername,
                masterAdminPassword
        )) {
            {
                KeycloakVoidResponse response = keycloakRestApi.createRealm(realmName, true);
                assertNotNull(response);
                assertEquals(201, response.statusCode);
                assertNotNull(response.location);
                // Keycloak does not return the created Realm
            }
            {
                KeycloakMapResponse response = keycloakRestApi.getRealmByName(realmName);
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                Map<String, Object> realm = response.body;
                logger.debug(keycloakRestApi.toString(realm));

                result = realm;
            }
        }

        return result;
    }

    protected void deleteRealm(String realmName) throws IOException {

        try(KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                keycloakUrl,
                keycloakMasterRealmName,
                masterAdminUsername,
                masterAdminPassword
        )) {
            {
                KeycloakVoidResponse response = keycloakRestApi.deleteRealm(realmName);
                assertNotNull(response);
                assertEquals(204, response.statusCode);
            }
        }
    }
}
