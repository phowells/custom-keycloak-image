package com.paulhowells.keycloak;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.paulhowells.keycloak.configurer.KeycloakConfigurer;
import com.paulhowells.keycloak.configurer.model.ClientDefinition;
import com.paulhowells.keycloak.configurer.model.KeycloakDefinition;
import com.paulhowells.keycloak.configurer.model.RealmDefinition;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModelTests {
    private static final Logger logger = LoggerFactory.getLogger(ModelTests.class);

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Test
    public void test() throws JsonProcessingException {
        logger.debug("<test");

        KeycloakDefinition keycloakDefinition = new KeycloakDefinition();

        RealmDefinition realmDefinition = new RealmDefinition();
        realmDefinition.setRealmName("test_realm");
        keycloakDefinition.getRealms().add(realmDefinition);

        {
            ClientDefinition clientDefinition = new ClientDefinition();
            clientDefinition.setRealmName("test_realm");
            clientDefinition.setClientId("app-ui");
            clientDefinition.setClientSecret("password");
            ClientDefinition.GrantTypes grantTypes = new ClientDefinition.GrantTypes();
            ClientDefinition.GrantTypes.Password password = new ClientDefinition.GrantTypes.Password();
            password.setEnabled(Boolean.TRUE);
            password.setFlowOverride("flow-override");
            grantTypes.setPassword(password);
            ClientDefinition.GrantTypes.ClientCredentials clientCredentials = new ClientDefinition.GrantTypes.ClientCredentials();
            clientCredentials.setEnabled(Boolean.TRUE);
            grantTypes.setClientCredentials(clientCredentials);
            ClientDefinition.GrantTypes.AuthorizationCode authorizationCode = new ClientDefinition.GrantTypes.AuthorizationCode();
            authorizationCode.setEnabled(Boolean.TRUE);
            List<String> redirectUris = new ArrayList<>();
            redirectUris.add("https://redirect.com");
            authorizationCode.setPostLoginRedirectUris(redirectUris);
            List<String> webOrigins = new ArrayList<>();
            webOrigins.add("app.com");
            authorizationCode.setWebOrigins(webOrigins);
            authorizationCode.setFlowOverride("flow-override");
            grantTypes.setAuthorizationCode(authorizationCode);
            clientDefinition.setGrantTypes(grantTypes);
            ClientDefinition.AdvancedSettings advancedSettings = new ClientDefinition.AdvancedSettings();
            // How long an access token can be used before it expires
            advancedSettings.setAccessTokenLifespanSeconds(5 * 60L); // 5 minutes
            // How long a session will persist after a token is issued (reset after each token refresh)
            advancedSettings.setClientSessionIdleTimeoutSeconds(20 * 60L); // 20 minutes
            // How long a session will persist before a token can no longer be refreshed
            advancedSettings.setClientSessionMaxLifespanSeconds(120 * 60L); // 2 hours
            // How long a session will persist after a token is issued (reset after each token refresh)
            advancedSettings.setClientOfflineSessionIdleTimeoutSeconds(24 * 60 * 60L); // one day
            // How long a session will persist before a token can no longer be refreshed
            advancedSettings.setClientOfflineSessionMaxLifespanSeconds(7 * 24 * 60 * 60L); // seven days
            clientDefinition.setAdvancedSettings(advancedSettings);
            keycloakDefinition.getClients().add(clientDefinition);
        }

        String keycloakDefinitionYaml = mapper.writeValueAsString(keycloakDefinition);
        logger.debug("\n{}", keycloakDefinitionYaml);

        logger.debug(">test");
    }

    @Test
    public void paulhowellsTest() throws IOException {

        try (KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                "https://keycloak.paulhowells.dev",
                "master",
                "admin",
                "admin"
        )) {

            KeycloakDefinition definition = KeycloakConfigurer.getDefinition(keycloakRestApi);

            String yaml = mapper.writeValueAsString(definition);
            logger.info("\n{}", yaml);
        }
    }

    @Test
    public void devTest() throws IOException {

        try (KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                "https://keycloak-dev.geotab.com",
                "master",
                "admin",
                "_firstkeyrelease_2024"
        )) {

            KeycloakDefinition definition = KeycloakConfigurer.getDefinition(keycloakRestApi);

            String yaml = mapper.writeValueAsString(definition);
            logger.info("\n{}", yaml);
        }
    }
}
