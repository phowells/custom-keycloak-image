package com.paulhowells.keycloak.theme;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.theme.ThemeSelectorProvider;
import org.keycloak.theme.ThemeSelectorProviderFactory;

public class DomainThemeSelectorProviderFactory implements ThemeSelectorProviderFactory {

    public static final String PROVIDER_ID = "default";

    @Override
    public ThemeSelectorProvider create(KeycloakSession keycloakSession) {
        return new DomainThemeSelectorProvider(keycloakSession);
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
