package com.paulhowells.keycloak.saml2bearer;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProvider;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.common.VerificationException;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.events.Errors;
import org.keycloak.models.*;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ValidateSaml2Authenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(ValidateSaml2Authenticator.class);

    protected static final String IDENTITY_PROVIDER_USER_ATTRIBUTE_NAME = "identity-provider-name";

    private final KeycloakSession keycloakSession;
    private final DestinationValidator destinationValidator;

    public ValidateSaml2Authenticator(
            KeycloakSession session,
            DestinationValidator destinationValidator
    ) {

        this.keycloakSession = session;
        this.destinationValidator = destinationValidator;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.debug("<authenticate");

        String saml2Response = retrieveSaml2Response(context);
        boolean valid = isValid(context, saml2Response);
        if (!valid) {
            context.getEvent().user(context.getUser());
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "invalid_grant", "Invalid user credentials");
            context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
        } else {
            context.getAuthenticationSession().setAuthNote(AuthenticationManager.PASSWORD_VALIDATED, "true");
            context.success();
        }

        logger.debug(">authenticate");

    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {
        // do nothing
    }

    public boolean isValid(AuthenticationFlowContext context, String saml2Response) {
        logger.debug("<isValid");
        boolean result = false;

        UserModel user = context.getUser();

        String identityProviderName = user.getFirstAttribute(IDENTITY_PROVIDER_USER_ATTRIBUTE_NAME);
        if (identityProviderName == null || identityProviderName.isBlank()) {

            logger.errorf("No identity provider attribute for user %s", user.getUsername());
        } else {

            Optional<IdentityProviderModel> identityProviderModelOption = context.getRealm().getIdentityProvidersStream()
                    .filter(IdentityProviderModel::isEnabled)
                    .filter(identityProvider -> Objects.equals(identityProviderName, identityProvider.getAlias()))
                    .findFirst();

            if (identityProviderModelOption.isPresent()) {

                IdentityProviderModel identityProviderModel = identityProviderModelOption.get();

                if ("saml".equals(identityProviderModel.getProviderId())) {

                    Set<SAMLIdentityProvider> providers = this.keycloakSession.getAllProviders(SAMLIdentityProvider.class);
                    SAMLIdentityProvider samlIdentityProvider = null;
                    for (SAMLIdentityProvider provider: providers) {
                        logger.debug("provider="+provider);

                        if (provider.getConfig().getAlias().equals(identityProviderName)) {
                            samlIdentityProvider = provider;
                            break;
                        }
                    }

//                    SAMLIdentityProvider samlIdentityProvider = this.keycloakSession.getProvider(SAMLIdentityProvider.class, identityProviderName);
                    logger.debug("samlIdentityProvider="+samlIdentityProvider);

                    SAMLIdentityProviderConfig config = new SAMLIdentityProviderConfig(identityProviderModel);

                    InternalSamlEndpoint internalSamlEndpoint = new InternalSamlEndpoint(
                            keycloakSession,
                            samlIdentityProvider,
                            config,
                            null,
                            destinationValidator
                    );

                    result = internalSamlEndpoint.handleLoginResponse(saml2Response);

                } else {

                    logger.warnf("User %s does not use a SAML Identity Provider", user.getUsername());
                }

            } else {
                logger.errorf("No identity provider matching %s for user %s", identityProviderName, user.getUsername());
            }
        }

        logger.debug("<isValid "+result);
        return result;
    }

    private static class InternalSamlEndpoint extends SAMLEndpoint {

        private final KeycloakSession keycloakSession;
        private final DestinationValidator destinationValidator;

        public InternalSamlEndpoint(
                KeycloakSession session,
                SAMLIdentityProvider provider,
                SAMLIdentityProviderConfig config,
                IdentityProvider.AuthenticationCallback callback,
                DestinationValidator destinationValidator
        ) {
            super(session, provider, config, callback, destinationValidator);

            this.keycloakSession = session;
            this.destinationValidator = destinationValidator;
        }

        boolean handleLoginResponse(
                String samlResponse
        ) {

            Binding binding = new Binding();

            Response.Status status = binding.handleSamlResponse(samlResponse);

            return Response.Status.OK.equals(status);
        }

        protected class Binding extends PostBinding {

            public Response.Status handleSamlResponse(String samlResponse) {
                SAMLDocumentHolder holder = this.extractResponseDocument(samlResponse);
                if (holder == null) {
                    logger.warn("invalid_saml_response invalid_saml_document");
                    return Response.Status.BAD_REQUEST;
                } else {
                    StatusResponseType statusResponse = (StatusResponseType)holder.getSamlObject();
                    if (this.isDestinationRequired() && statusResponse.getDestination() == null && this.containsUnencryptedSignature(holder)) {
                        logger.warn("invalid_saml_response missing_required_destination");
                        return Response.Status.BAD_REQUEST;
                    } else if (!InternalSamlEndpoint.this.destinationValidator.validate(this.getExpectedDestination(InternalSamlEndpoint.this.config.getAlias()), statusResponse.getDestination())) {
                        logger.warn("invalid_saml_response invalid_saml_response");
                        return Response.Status.BAD_REQUEST;
                    } else {
                        if (InternalSamlEndpoint.this.config.isValidateSignature()) {
                            try {
                                this.verifySignature("SAMLResponse", holder);
                            } catch (VerificationException var7) {
                                logger.warn("validation failed invalid_signature");
                                return Response.Status.BAD_REQUEST;
                            }
                        }

                        logger.debug("SAML2 Login Successful");
                        return Response.Status.OK;
                    }
                }
            }

            private String getExpectedDestination(String providerAlias) {
                return Urls.identityProviderAuthnResponse(InternalSamlEndpoint.this.keycloakSession.getContext().getUri().getBaseUri(), providerAlias, InternalSamlEndpoint.this.realm.getName()).toString();
            }
        }
    }

    public Response errorResponse(int status, String error, String errorDescription) {
        OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(error, errorDescription);
        return Response.status(status).entity(errorRep).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    protected String retrieveSaml2Response(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
        return inputData.getFirst("saml2_response");
    }

    @Override
    public void close() {

    }
}
