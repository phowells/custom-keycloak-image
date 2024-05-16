package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.TokenSettings;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.Realm;
import org.slf4j.Logger;


public class TokenSettingsConfigurer {

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    TokenSettingsConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
    }

    boolean updateTokenSettings(
            Realm realm,
            TokenSettings definition
    ) {
        logger.debug("<updateTokenSettings");
        boolean result = false;

        realm = keycloakRestApi.getRealmByName(realm.getRealm());

        TokenSettings current = getDefinition(realm);

        logger.info("Checking realm {} token settings for updates", realm.getRealm());

        if (isDirty(current, definition)) {

            logger.info("Updating realm {} token settings", realm.getRealm());

            applyDefinition(
                    definition,
                    realm
            );

            keycloakRestApi.updateRealm(realm.getRealm(), realm);

            realm = keycloakRestApi.getRealmByName(realm.getRealm());
        } else {

            logger.info("No Change");
        }
        logger.debug(">updateTokenSettings {}", result);
        return result;
    }

    private boolean isDirty(
            TokenSettings current,
            TokenSettings updated) {
        return updated!=null && !current.isUnchanged(updated, null, logger);
    }

    TokenSettings getDefinition(Realm realm) {
        TokenSettings result = new TokenSettings();

        result.setDefaultSignatureAlgorithm(realm.getDefaultSignatureAlgorithm());
        result.setRevokeRefreshToken(realm.getRevokeRefreshToken());
        result.setRefreshTokenMaxReuse(realm.getRefreshTokenMaxReuse());
        result.setAccessTokenLifespanSeconds(realm.getAccessTokenLifespan());
        result.setClientLoginTimeoutSeconds(realm.getAccessCodeLifespan());
        result.setActionTokenGeneratedByUserLifespanSeconds(realm.getActionTokenGeneratedByUserLifespan());
        result.setActionTokenGeneratedByAdminLifespanSeconds(realm.getActionTokenGeneratedByAdminLifespan());

        return result;
    }

    private void applyDefinition(TokenSettings definition, Realm realm) {

        realm.setDefaultSignatureAlgorithm(definition.getDefaultSignatureAlgorithm());
        realm.setRevokeRefreshToken(definition.getRevokeRefreshToken());
        realm.setRefreshTokenMaxReuse(definition.getRefreshTokenMaxReuse());
        realm.setAccessTokenLifespan(definition.getAccessTokenLifespanSeconds());
        realm.setAccessCodeLifespan(definition.getClientLoginTimeoutSeconds());
        realm.setActionTokenGeneratedByUserLifespan(definition.getActionTokenGeneratedByUserLifespanSeconds());
        realm.setActionTokenGeneratedByAdminLifespan(definition.getActionTokenGeneratedByAdminLifespanSeconds());
    }

}
