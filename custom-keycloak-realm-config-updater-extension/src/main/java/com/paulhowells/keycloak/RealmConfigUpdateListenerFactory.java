package com.paulhowells.keycloak;

import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.PostMigrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmConfigUpdateListenerFactory implements EventListenerProviderFactory {

    private static final Logger logger = LoggerFactory.getLogger(RealmConfigUpdateListenerFactory.class);

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new RealmConfigUpdateListener();
    }

    @Override
    public void init(org.keycloak.Config.Scope scope) {
        logger.info("<init");
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        logger.info("<postInit");
        keycloakSessionFactory.register(event -> {

            logger.info("Event Triggered: " + event.getClass().getName());

            if (event instanceof PostMigrationEvent) {

                logger.info("PostMigrationEvent:  Importing configuration");
            }
        });
    }

    @Override
    public void close() {
        logger.info("<close");

    }

    @Override
    public String getId() {
        return "realm-config-update-listener";
    }
}
