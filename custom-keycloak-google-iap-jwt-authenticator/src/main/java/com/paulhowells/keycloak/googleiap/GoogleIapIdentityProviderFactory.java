package com.paulhowells.keycloak.googleiap;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

public class GoogleIapIdentityProviderFactory  extends AbstractIdentityProviderFactory<GoogleIapIdentityProvider> implements IdentityProviderFactory<GoogleIapIdentityProvider> {

    private static final Logger logger = Logger.getLogger(GoogleIapIdentityProviderFactory.class);

    public static final String PROVIDER_ID = "google-iap";

    @Override
    public String getName() {
        return "Google IAP Provider";
    }

    @Override
    public GoogleIapIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new GoogleIapIdentityProvider(session);
    }

    @Override
    public IdentityProviderModel createConfig() {
        logger.info("<getConfig");
        IdentityProviderModel result = new IdentityProviderModel();

        logger.info(">getConfig " + result);
        return result;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}