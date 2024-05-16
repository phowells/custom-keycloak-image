package com.paulhowells.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.paulhowells.keycloak.configurer.KeycloakConfigurer;
import com.paulhowells.keycloak.configurer.model.KeycloakDefinition;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ConfigurerTests {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurerTests.class);

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Test
    @SetEnvironmentVariable(key = KeycloakConfigurer.KEYCLOAK_URL_ENV_VARIABLE, value = "https://keycloak.paulhowells.dev")
    @SetEnvironmentVariable(key = KeycloakConfigurer.MASTER_REALM_ADMIN_USERNAME_ENV_VARIABLE, value = "admin")
    @SetEnvironmentVariable(key = KeycloakConfigurer.MASTER_REALM_ADMIN_PASSWORD_ENV_VARIABLE, value = "admin")
    public void configPaulHowellsDev() throws IOException {
        logger.debug("<configPaulHowellsDev");

        {
            URL configUrl = ConfigurerTests.class.getResource("/keycloak-config");
            logger.debug("configUrl={}", configUrl);
            assertNotNull(configUrl);

            String[] args = {
                    String.format("--config=%s", configUrl.getPath())
            };

            KeycloakConfigurer.main(args);
        }
        logger.debug(">configPaulHowellsDev");
    }

    @Test
    @SetEnvironmentVariable(key = KeycloakConfigurer.KEYCLOAK_URL_ENV_VARIABLE, value = "https://keycloak.paulhowells.dev")
    @SetEnvironmentVariable(key = KeycloakConfigurer.MASTER_REALM_ADMIN_USERNAME_ENV_VARIABLE, value = "admin")
    @SetEnvironmentVariable(key = KeycloakConfigurer.MASTER_REALM_ADMIN_PASSWORD_ENV_VARIABLE, value = "admin")
    public void test() throws IOException {
        logger.debug("<test");

        {
            URL configUrl = ConfigurerTests.class.getResource("/configTest1");
            logger.debug("configUrl={}", configUrl);
            assertNotNull(configUrl);

            String[] args = {
                    String.format("--config=%s", configUrl.getPath())
            };

            KeycloakConfigurer.main(args);
        }

        {
            URL configUrl = ConfigurerTests.class.getResource("/configTest2");
            logger.debug("configUrl={}", configUrl);
            assertNotNull(configUrl);

            String[] args = {
                    String.format("--config=%s", configUrl.getPath())
            };

            KeycloakConfigurer.main(args);
        }

        {
            URL configUrl = ConfigurerTests.class.getResource("/configTest3");
            logger.debug("configUrl={}", configUrl);
            assertNotNull(configUrl);

            String[] args = {
                    String.format("--config=%s", configUrl.getPath())
            };

            KeycloakConfigurer.main(args);
        }

        logger.debug(">test");
    }

    @Test
    @SetEnvironmentVariable(key = KeycloakConfigurer.KEYCLOAK_URL_ENV_VARIABLE, value = "https://keycloak.paulhowells.dev")
    @SetEnvironmentVariable(key = KeycloakConfigurer.MASTER_REALM_ADMIN_USERNAME_ENV_VARIABLE, value = "admin")
    @SetEnvironmentVariable(key = KeycloakConfigurer.MASTER_REALM_ADMIN_PASSWORD_ENV_VARIABLE, value = "admin")
    public void deleteTestConfig() throws IOException {
        logger.debug("<deleteTestConfig");

        {
            URL configUrl = ConfigurerTests.class.getResource("/deleteTestConfig");
            logger.debug("configUrl={}", configUrl);
            assertNotNull(configUrl);

            String[] args = {
                    String.format("--config=%s", configUrl.getPath())
            };

            KeycloakConfigurer.main(args);
        }

        logger.debug(">deleteTestConfig");
    }
}
