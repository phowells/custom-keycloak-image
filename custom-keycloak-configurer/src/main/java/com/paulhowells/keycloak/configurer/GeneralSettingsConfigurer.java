package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.GeneralSettings;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.Realm;
import org.slf4j.Logger;


public class GeneralSettingsConfigurer {

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    GeneralSettingsConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
    }

    boolean updateGeneralSettings(
            Realm realm,
            GeneralSettings definition
    ) {
        logger.debug("<updateGeneralSettings");
        boolean result = false;

        realm = keycloakRestApi.getRealmByName(realm.getRealm());

        GeneralSettings current = getDefinition(realm);

        logger.info("Checking realm {} general settings for updates", realm.getRealm());

        if (isDirty(current, definition)) {

            logger.info("Updating realm {} general settings", realm.getRealm());

            applyDefinition(
                    definition,
                    realm
            );

            keycloakRestApi.updateRealm(realm.getRealm(), realm);
        } else {

            logger.info("No Change");
        }
        logger.debug(">updateGeneralSettings {}", result);
        return result;
    }

    private boolean isDirty(
            GeneralSettings current,
            GeneralSettings updated) {
        return updated!=null && !current.isUnchanged(updated, null, logger);
    }

    GeneralSettings getDefinition(Realm realm) {
        GeneralSettings result = new GeneralSettings();

        result.setUserManagedAccessAllowed(realm.getUserManagedAccessAllowed());

        return result;
    }

    private void applyDefinition(GeneralSettings definition, Realm realm) {

        realm.setUserManagedAccessAllowed(definition.getUserManagedAccessAllowed());
    }

}
