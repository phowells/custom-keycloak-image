package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.PasswordPolicy;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.Realm;
import org.slf4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PasswordPolicyConfigurer {

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    PasswordPolicyConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
    }

    boolean updatePasswordPolicy(
            Realm realm,
            PasswordPolicy definition
    ) {
        logger.debug("<updatePasswordPolicy");
        boolean result = false;

        realm = keycloakRestApi.getRealmByName(realm.getRealm());

        PasswordPolicy current = getDefinition(realm);

        logger.info("Checking realm {} password policy for updates", realm.getRealm());

        if (isDirty(current, definition)) {

            logger.info("Updating realm {} password policy", realm.getRealm());

            String passwordPolicy = getPasswordPolicy(definition);

            realm.setPasswordPolicy(passwordPolicy);

            keycloakRestApi.updateRealm(realm.getRealm(), realm);
        } else {

            logger.info("No Change");
        }
        logger.debug(">updatePasswordPolicy {}", result);
        return result;
    }

    private boolean isDirty(
            PasswordPolicy current,
            PasswordPolicy updated) {
        return updated!=null && !current.isUnchanged(updated, null, logger);
    }

    private String getPasswordPolicy(PasswordPolicy definition) {
        List<String> policies = new ArrayList<>();

        if (definition.getMinimumLength() !=null) {

            policies.add(String.format("length(%s)", definition.getMinimumLength()));
        }

        if (definition.getDigits() !=null) {

            policies.add(String.format("digits(%s)", definition.getDigits()));
        }

        if (definition.getLowercase() !=null) {

            policies.add(String.format("lowerCase(%s)", definition.getLowercase()));
        }

        if (definition.getUppercase() !=null) {

            policies.add(String.format("upperCase(%s)", definition.getUppercase()));
        }

        if (definition.getSpecialCharacters() !=null) {

            policies.add(String.format("specialChars(%s)", definition.getSpecialCharacters()));
        }

        Collections.sort(policies);

        return String.join(" and ", policies);
    }

    PasswordPolicy getDefinition(Realm realm) {
        PasswordPolicy result = new PasswordPolicy();

        String passwordPolicy = realm.getPasswordPolicy();

        if (passwordPolicy !=null && !passwordPolicy.isBlank()) {

            {
                Pattern pattern = Pattern.compile(".*length\\(([0-9]+)\\).*");
                Matcher matcher = pattern.matcher(passwordPolicy);
                if (matcher.matches()) {

                    String value = matcher.group(1);
                    result.setMinimumLength(Integer.valueOf(value));
                }
            }

            {
                Pattern pattern = Pattern.compile(".*digits\\(([0-9]+)\\).*");
                Matcher matcher = pattern.matcher(passwordPolicy);
                if (matcher.matches()) {

                    String value = matcher.group(1);
                    result.setDigits(Integer.valueOf(value));
                }
            }

            {
                Pattern pattern = Pattern.compile(".*lowerCase\\(([0-9]+)\\).*");
                Matcher matcher = pattern.matcher(passwordPolicy);
                if (matcher.matches()) {

                    String value = matcher.group(1);
                    result.setLowercase(Integer.valueOf(value));
                }
            }

            {
                Pattern pattern = Pattern.compile(".*upperCase\\(([0-9]+)\\).*");
                Matcher matcher = pattern.matcher(passwordPolicy);
                if (matcher.matches()) {

                    String value = matcher.group(1);
                    result.setUppercase(Integer.valueOf(value));
                }
            }

            {
                Pattern pattern = Pattern.compile(".*specialChars\\(([0-9]+)\\).*");
                Matcher matcher = pattern.matcher(passwordPolicy);
                if (matcher.matches()) {

                    String value = matcher.group(1);
                    result.setSpecialCharacters(Integer.valueOf(value));
                }
            }
        }

        return result;
    }
}
