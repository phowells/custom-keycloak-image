package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.EventSettings;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.EventsConfig;
import com.paulhowells.keycloak.configurer.rest.client.model.Realm;
import org.slf4j.Logger;

import java.util.Collections;


public class EventSettingsConfigurer {

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    EventSettingsConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
    }

    boolean updateEventSettings(
            Realm realm,
            EventSettings definition
    ) {
        logger.debug("<updateEventSettings");
        boolean result = false;

        realm = keycloakRestApi.getRealmByName(realm.getRealm());

        EventsConfig eventsConfig = keycloakRestApi.getEventsConfig(realm.getRealm());

        EventSettings current = getDefinition(realm, eventsConfig);

        logger.info("Checking realm {} email settings for updates", realm.getRealm());

        if (isDirty(current, definition)) {

            logger.info("Updating realm {} email settings", realm.getRealm());

            applyDefinition(
                    definition,
                    realm,
                    eventsConfig
            );

            keycloakRestApi.updateRealm(realm.getRealm(), realm);
            keycloakRestApi.updateEventsConfig(realm.getRealm(), eventsConfig);
        } else {

            logger.info("No Change");
        }
        logger.debug(">updateEventSettings {}", result);
        return result;
    }

    private boolean isDirty(
            EventSettings current,
            EventSettings updated) {
        return updated!=null && !current.isUnchanged(updated, null, logger);
    }

    EventSettings getDefinition(Realm realm) {

        EventsConfig eventsConfig = keycloakRestApi.getEventsConfig(realm.getRealm());

        return getDefinition(realm, eventsConfig);
    }

    private EventSettings getDefinition(Realm realm, EventsConfig eventsConfig) {
        EventSettings result = new EventSettings();

        result.setEventsListeners(eventsConfig.getEventsListeners());
        result.setUserEventsEnabled(Boolean.TRUE.equals(eventsConfig.getEventsEnabled()));

        Integer userEventsExpirationSeconds = null;
        String userEventsExpiration = eventsConfig.getEventsExpiration();
        if (userEventsExpiration != null && !userEventsExpiration.isBlank()) {
            userEventsExpirationSeconds = Integer.valueOf(userEventsExpiration);
        }
        result.setUserEventsExpirationSeconds(userEventsExpirationSeconds);

        // Sort the events because they are not returned in a deterministic order
        Collections.sort(eventsConfig.getEnabledEventTypes());
        result.setEnabledEventTypes(eventsConfig.getEnabledEventTypes());
        result.setAdminEventsEnabled(Boolean.TRUE.equals(eventsConfig.getAdminEventsEnabled()));
        result.setAdminEventsDetailsEnabled(Boolean.TRUE.equals(eventsConfig.getAdminEventsDetailsEnabled()));

        Integer adminEventsExpirationSeconds = null;
        String adminEventsExpiration = realm.getAttributes().get("adminEventsExpiration");
        if (adminEventsExpiration != null && !adminEventsExpiration.isBlank()) {
            adminEventsExpirationSeconds = Integer.valueOf(adminEventsExpiration);
        }
        result.setAdminEventsExpirationSeconds(adminEventsExpirationSeconds);



        return result;
    }

    private void applyDefinition(EventSettings definition, Realm realm, EventsConfig eventsConfig) {

        eventsConfig.setEventsListeners(definition.getEventsListeners());
        eventsConfig.setEventsEnabled(Boolean.TRUE.equals(definition.getUserEventsEnabled()));
        eventsConfig.setEventsExpiration(definition.getUserEventsExpirationSeconds()==null?null:definition.getUserEventsExpirationSeconds().toString());
        eventsConfig.setEnabledEventTypes(definition.getEnabledEventTypes());
        eventsConfig.setAdminEventsEnabled(Boolean.TRUE.equals(definition.getAdminEventsEnabled()));
        eventsConfig.setAdminEventsDetailsEnabled(Boolean.TRUE.equals(definition.getAdminEventsDetailsEnabled()));

        String value = definition.getAdminEventsExpirationSeconds()==null?null:definition.getAdminEventsExpirationSeconds().toString();
        realm.getAttributes().put("adminEventsExpiration", value);
    }

}
