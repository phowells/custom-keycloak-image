package com.paulhowells.keycloak.theme;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.theme.Theme;
import org.keycloak.theme.ThemeSelectorProvider;

import java.net.URI;

public class DomainThemeSelectorProvider implements ThemeSelectorProvider {

    private static final Logger logger = Logger.getLogger(DomainThemeSelectorProvider.class);

    private final KeycloakSession session;

    public DomainThemeSelectorProvider(KeycloakSession keycloakSession) {
        logger.info("<DomainThemeSelectorProvider");

        this.session = keycloakSession;

        logger.info(">DomainThemeSelectorProvider");
    }

    @Override
    public String getThemeName(Theme.Type type) {
        logger.infof("<getThemeName %s", type);
        String name = null;

        KeycloakContext context = this.session.getContext();
        logger.infof("context=%s", context);
        KeycloakUriInfo uri = context.getUri();
        logger.infof("uri=%s", uri);
        URI requestUri = uri.getRequestUri();
        logger.infof("requestUri=%s", requestUri);
        String host = requestUri.getHost();
        logger.infof("host=%s", host);

        name = getHostThemeName(type, host);
        logger.infof("name=%s", name);

        if (name == null || name.isEmpty()) {
            name = getDefaultThemeName(type);
        }

        logger.infof(">DomainThemeSelectorProvider %s", name);
        return name;
    }

    String getHostThemeName(Theme.Type type, String host) {
        logger.infof("<getHostThemeName %s", type);

        String name = Config.scope(new String[]{"theme"}).get("default");
        logger.infof("name=%s", name);
        if (name == null || name.isEmpty()) {
            name = host;
        }

        logger.infof(">getHostThemeName %s", name);
        return name;
    }

    @Override
    public void close() {

    }
}
