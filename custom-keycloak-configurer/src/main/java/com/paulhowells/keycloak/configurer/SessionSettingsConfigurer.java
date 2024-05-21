package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.SessionSettings;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.Realm;
import com.paulhowells.keycloak.configurer.rest.client.model.SmtpServer;
import org.slf4j.Logger;


public class SessionSettingsConfigurer {

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    SessionSettingsConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
    }

    boolean updateSessionSettings(
            Realm realm,
            SessionSettings definition
    ) {
        logger.debug("<updateSessionSettings");
        boolean result = false;

        realm = keycloakRestApi.getRealmByName(realm.getRealm());

        SessionSettings current = getDefinition(realm);

        logger.info("Checking realm {} session settings for updates", realm.getRealm());

        if (isDirty(current, definition)) {

            logger.info("Updating realm {} session settings", realm.getRealm());

            applyDefinition(
                    definition,
                    realm
            );

            keycloakRestApi.updateRealm(realm.getRealm(), realm);

            realm = keycloakRestApi.getRealmByName(realm.getRealm());
        } else {

            logger.info("No Change");
        }
        logger.debug(">updateSessionSettings {}", result);
        return result;
    }

    private boolean isDirty(
            SessionSettings current,
            SessionSettings updated) {
        return updated!=null && !current.isUnchanged(updated, null, logger);
    }

    SessionSettings getDefinition(Realm realm) {
        SessionSettings result = new SessionSettings();

        result.setSsoSessionIdleTimeoutSeconds(realm.getSsoSessionIdleTimeout());
        result.setSsoSessionMaxLifespanSeconds(realm.getSsoSessionMaxLifespan());
        result.setSsoSessionIdleTimeoutRememberMeSeconds(realm.getSsoSessionIdleTimeoutRememberMe());;
        result.setSsoSessionMaxLifespanRememberMeSeconds(realm.getSsoSessionMaxLifespanRememberMe());
        result.setClientSessionIdleTimeoutSeconds(realm.getClientSessionIdleTimeout());
        result.setClientSessionMaxLifespanSeconds(realm.getClientSessionMaxLifespan());
        result.setOfflineSessionIdleTimeoutSeconds(realm.getOfflineSessionIdleTimeout());
        result.setOfflineSessionMaxLifespanEnabled(Boolean.TRUE.equals(realm.getOfflineSessionMaxLifespanEnabled()));
        result.setOfflineSessionMaxLifespanSeconds(realm.getOfflineSessionMaxLifespan());
        result.setAccessCodeLifespanLoginSeconds(realm.getAccessCodeLifespanLogin());
        result.setAccessCodeLifespanUserActionSeconds(realm.getAccessCodeLifespanUserAction());

        return result;
    }

    private void applyDefinition(SessionSettings definition, Realm realm) {

        realm.setSsoSessionIdleTimeout(definition.getSsoSessionIdleTimeoutSeconds());
        realm.setSsoSessionMaxLifespan(definition.getSsoSessionMaxLifespanSeconds());
        realm.setSsoSessionIdleTimeoutRememberMe(definition.getSsoSessionIdleTimeoutRememberMeSeconds());;
        realm.setSsoSessionMaxLifespanRememberMe(definition.getSsoSessionMaxLifespanRememberMeSeconds());
        realm.setClientSessionIdleTimeout(definition.getClientSessionIdleTimeoutSeconds());
        realm.setClientSessionMaxLifespan(definition.getClientSessionMaxLifespanSeconds());
        realm.setOfflineSessionIdleTimeout(definition.getOfflineSessionIdleTimeoutSeconds());
        realm.setOfflineSessionMaxLifespanEnabled(Boolean.TRUE.equals(definition.getOfflineSessionMaxLifespanEnabled()));
        realm.setOfflineSessionMaxLifespan(definition.getOfflineSessionMaxLifespanSeconds());
        realm.setAccessCodeLifespanLogin(definition.getAccessCodeLifespanLoginSeconds());
        realm.setAccessCodeLifespanUserAction(definition.getAccessCodeLifespanUserActionSeconds());
    }

}
