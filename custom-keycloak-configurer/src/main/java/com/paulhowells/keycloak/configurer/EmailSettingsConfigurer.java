package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.EmailSettings;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.Realm;
import com.paulhowells.keycloak.configurer.rest.client.model.SmtpServer;
import org.slf4j.Logger;


public class EmailSettingsConfigurer {

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    EmailSettingsConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
    }

    boolean updateEmailSettings(
            Realm realm,
            EmailSettings definition
    ) {
        logger.debug("<updateEmailSettings");
        boolean result = false;

        realm = keycloakRestApi.getRealmByName(realm.getRealm());

        EmailSettings current = getDefinition(realm);

        logger.info("Checking realm {} email settings for updates", realm.getRealm());

        if (isDirty(current, definition)) {

            logger.info("Updating realm {} email settings", realm.getRealm());

            applyDefinition(
                    definition,
                    realm
            );

            keycloakRestApi.updateRealm(realm.getRealm(), realm);
        } else {

            logger.info("No Change");
        }
        logger.debug(">updateEmailSettings {}", result);
        return result;
    }

    private boolean isDirty(
            EmailSettings current,
            EmailSettings updated) {
        return updated!=null && !current.isUnchanged(updated, null, logger);
    }

    EmailSettings getDefinition(Realm realm) {
        EmailSettings result = new EmailSettings();

        SmtpServer smtpServer = realm.getSmtpServer();

        if (smtpServer != null) {

            result.setFromAddress(smtpServer.getFrom());
            result.setFromDisplayName(smtpServer.getFromDisplayName());
            result.setReplyToAddress(smtpServer.getReplyTo());
            result.setReplyToDisplayName(smtpServer.getReplyToDisplayName());
            result.setEnvelopeFromAddress(smtpServer.getEnvelopeFrom());
            result.setHost(smtpServer.getHost());
            result.setPort(smtpServer.getPort()==null?null:Integer.valueOf(smtpServer.getPort()));
            result.setEnableSsl(Boolean.valueOf(smtpServer.getSsl()));
            result.setEnableStartTls(Boolean.valueOf(smtpServer.getStarttls()));
            result.setAuthenticationEnabled(Boolean.valueOf(smtpServer.getAuth()));
            result.setUsername(smtpServer.getUser());
            result.setPassword(smtpServer.getPassword());
        }

        return result;
    }

    private void applyDefinition(EmailSettings definition, Realm realm) {

        SmtpServer smtpServer = new SmtpServer();

        smtpServer.setFrom(definition.getFromAddress());
        smtpServer.setFromDisplayName(definition.getFromDisplayName());
        smtpServer.setReplyTo(definition.getReplyToAddress());
        smtpServer.setReplyToDisplayName(definition.getReplyToDisplayName());
        smtpServer.setEnvelopeFrom(definition.getEnvelopeFromAddress());
        smtpServer.setHost(definition.getHost());
        smtpServer.setPort(definition.getPort()==null?null:definition.getPort().toString());
        smtpServer.setSsl((definition.getEnableSsl()==null?Boolean.FALSE:definition.getEnableSsl()).toString());
        smtpServer.setStarttls((definition.getEnableStartTls()==null?Boolean.FALSE:definition.getEnableStartTls()).toString());
        smtpServer.setAuth((definition.getAuthenticationEnabled()==null?Boolean.FALSE:definition.getAuthenticationEnabled()).toString());
        smtpServer.setUser(definition.getUsername());
        smtpServer.setPassword(definition.getPassword());

        realm.setSmtpServer(smtpServer);
    }

}
