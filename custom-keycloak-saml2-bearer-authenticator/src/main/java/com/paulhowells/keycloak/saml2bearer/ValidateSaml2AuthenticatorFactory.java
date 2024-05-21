package com.paulhowells.keycloak.saml2bearer;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.saml.validators.DestinationValidator;

import java.util.LinkedList;
import java.util.List;

public class ValidateSaml2AuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "direct-grant-validate-saml2";

    private DestinationValidator destinationValidator;

    @Override
    public Authenticator create(KeycloakSession session) {

        return new ValidateSaml2Authenticator(session, destinationValidator);
    }

    @Override
    public void init(Config.Scope config) {

        this.destinationValidator = DestinationValidator.forProtocolMap(config.getArray("knownProtocols"));
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }


    @Override
    public String getDisplayType() {
        return "SAML2 Response";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getHelpText() {
        return "Validates the SAML2 response supplied as a 'saml2_response' form parameter in direct grant request";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new LinkedList<>();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
