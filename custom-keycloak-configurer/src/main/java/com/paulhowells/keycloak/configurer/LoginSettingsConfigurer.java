package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.LoginSettings;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.Realm;
import org.slf4j.Logger;


public class LoginSettingsConfigurer {

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    LoginSettingsConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
    }

    boolean updateLoginSettings(
            Realm realm,
            LoginSettings definition
    ) {
        logger.debug("<updateLoginSettings");
        boolean result = false;

        realm = keycloakRestApi.getRealmByName(realm.getRealm());

        LoginSettings current = getDefinition(realm);

        logger.info("Checking realm {} login settings for updates", realm.getRealm());

        if (isDirty(current, definition)) {

            logger.info("Updating realm {} login settings", realm.getRealm());

            applyDefinition(
                    definition,
                    realm
            );

            keycloakRestApi.updateRealm(realm.getRealm(), realm);

            realm = keycloakRestApi.getRealmByName(realm.getRealm());
        } else {

            logger.info("No Change");
        }
        logger.debug(">updateLoginSettings {}", result);
        return result;
    }

    private boolean isDirty(
            LoginSettings current,
            LoginSettings updated) {
        return updated!=null && !current.isUnchanged(updated, null, logger);
    }

    LoginSettings getDefinition(Realm realm) {
        LoginSettings result = new LoginSettings();

        result.setRegistrationAllowed(realm.getRegistrationAllowed());
        result.setResetPasswordAllowed(realm.getResetPasswordAllowed());
        result.setRememberMe(realm.getRememberMe());
        result.setRegistrationEmailAsUsername(realm.getRegistrationEmailAsUsername());
        result.setLoginWithEmailAllowed(realm.getLoginWithEmailAllowed());
        result.setDuplicateEmailsAllowed(realm.getDuplicateEmailsAllowed());
        result.setVerifyEmail(realm.getVerifyEmail());
        result.setEditUsernameAllowed(realm.getEditUsernameAllowed());

        return result;
    }

    private void applyDefinition(LoginSettings definition, Realm realm) {

        realm.setRegistrationAllowed(definition.getRegistrationAllowed());
        realm.setResetPasswordAllowed(definition.getResetPasswordAllowed());
        realm.setRememberMe(definition.getRememberMe());
        realm.setRegistrationEmailAsUsername(definition.getRegistrationEmailAsUsername());
        realm.setLoginWithEmailAllowed(definition.getLoginWithEmailAllowed());
        realm.setDuplicateEmailsAllowed(definition.getDuplicateEmailsAllowed());
        realm.setVerifyEmail(definition.getVerifyEmail());
        realm.setEditUsernameAllowed(definition.getEditUsernameAllowed());
    }

}
