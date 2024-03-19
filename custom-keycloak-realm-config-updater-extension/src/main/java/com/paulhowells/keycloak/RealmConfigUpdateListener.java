package com.paulhowells.keycloak;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmConfigUpdateListener implements EventListenerProvider {

    private static final Logger logger = LoggerFactory.getLogger(RealmConfigUpdateListenerFactory.class);

    @Override
    public void onEvent(Event event) {
        logger.info("<onEvent "+event.getClass().getName());
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        logger.info("<onEvent "+adminEvent.getClass().getName());
    }

    @Override
    public void close() {
        logger.info("<close");
    }
}
