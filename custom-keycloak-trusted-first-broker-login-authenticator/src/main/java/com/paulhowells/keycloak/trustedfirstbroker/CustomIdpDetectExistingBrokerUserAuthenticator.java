package com.paulhowells.keycloak.trustedfirstbroker;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.ExistingUserInfo;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;

import java.util.*;

public class CustomIdpDetectExistingBrokerUserAuthenticator extends IdpCreateUserIfUniqueAuthenticator {

    private static final Logger logger = Logger.getLogger(CustomIdpDetectExistingBrokerUserAuthenticator.class);

    protected static final String IDENTITY_PROVIDER_USER_ATTRIBUTE_NAME = "identity-provider-name";

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        logger.debug("<authenticateImpl "+brokerContext);
        RealmModel realm = context.getRealm();

        String authNote = context.getAuthenticationSession().getAuthNote(EXISTING_USER_INFO);
        if (authNote != null) {
            context.attempted();
            logger.debug(">authenticateImpl "+ authNote);
            return;
        }

        String email = getEmailFromBrokerContext(brokerContext);
        logger.debug("attribute email="+email);

        // It may be possible that the external SAML provider will use some other attribute that we will need to use to uniquely identify the existing user.
        //  If there is variation between how the different SAML providers are configured we will need to set attributes on the IDP during migration and vary the logic here
        //  based on those attributes.
        logger.debug("Checking for existing user "+email);
        UserModel existingUser = getExistingUserForEmail(context, email, serializedCtx, brokerContext);

        if (existingUser == null) {
            logger.errorf("The user %s should be already registered in the realm to login %s",email,  realm.getName());
            Response challengeResponse = context.form()
                    .setError(Messages.FEDERATED_IDENTITY_UNAVAILABLE, email, brokerContext.getIdpConfig().getAlias())
                    .createErrorPage(Response.Status.UNAUTHORIZED);
            context.challenge(challengeResponse);
            context.getEvent()
                    .detail("authenticator", "DetectExistingBrokerUser")
                    .removeDetail(Details.AUTH_METHOD)
                    .removeDetail(Details.AUTH_TYPE)
                    .error(Errors.USER_NOT_FOUND);

        } else {
            logger.debugf("Duplication detected. There is already existing user with email '%s' .",
                    existingUser.getEmail());

            IdentityProviderModel authenticationIdpModel = brokerContext.getIdpConfig();
            String authenticationProviderId = authenticationIdpModel.getProviderId();

            // Confirm the SAML Issuer
            String identityProviderName = existingUser.getFirstAttribute(IDENTITY_PROVIDER_USER_ATTRIBUTE_NAME);

            Optional<IdentityProviderModel> userIdp = context.getRealm().getIdentityProvidersStream()
                    .filter(IdentityProviderModel::isEnabled)
                    .filter(identityProvider -> Objects.equals(identityProviderName, identityProvider.getAlias()))
                    .findFirst();

            if (userIdp.isPresent()) {

                IdentityProviderModel userIdpModel = userIdp.get();

                String userProviderId = userIdpModel.getProviderId();
                logger.debug("userProviderId="+userProviderId);
                logger.debug("authenticationProviderId="+authenticationProviderId);

                if (userProviderId.equals("saml") && authenticationProviderId.equals("saml")) {

                    String userIssuer = userIdpModel.getConfig().get("entityId");
                    logger.debug("userIssuer="+userIssuer);
                    String authenticationIssuer = authenticationIdpModel.getConfig().get("entityId");
                    logger.debug("authenticationIssuer="+authenticationIssuer);

                    if (userIssuer.equals(authenticationIssuer)) {

                        // Set duplicated user, so next authenticators can deal with it
                        context.getAuthenticationSession().setAuthNote(EXISTING_USER_INFO, new ExistingUserInfo(existingUser.getId(), "email", existingUser.getEmail()).serialize());

                        context.success();
                        logger.debug(">authenticateImpl success");
                        return;
                    }
                }
            }

            logger.errorf("The user %s is not registered to authenticate with IDP %s in realm %s",email,  authenticationProviderId,  realm.getName());
            Response challengeResponse = context.form()
                    .setError(Messages.FEDERATED_IDENTITY_UNAVAILABLE, email, brokerContext.getIdpConfig().getAlias())
                    .createErrorPage(Response.Status.UNAUTHORIZED);
            context.challenge(challengeResponse);
            context.getEvent()
                    .detail("authenticator", "Unauthorized Identity Provider")
                    .removeDetail(Details.AUTH_METHOD)
                    .removeDetail(Details.AUTH_TYPE)
                    .error(Errors.IDENTITY_PROVIDER_ERROR);
        }

        logger.debug(">authenticateImpl");
    }

    private String getEmailFromBrokerContext(BrokeredIdentityContext brokerContext) {
        String result = null;

        Map<String, Object> contextData = brokerContext.getContextData();

        org.keycloak.dom.saml.v2.assertion.AssertionType assertionType = (AssertionType) contextData.get("SAML_ASSERTION");

        outer: for(AttributeStatementType attributeStatementType: assertionType.getAttributeStatements()) {

            for(AttributeStatementType.ASTChoiceType astChoiceType: attributeStatementType.getAttributes()) {

                AttributeType attribute = astChoiceType.getAttribute();

                logger.debug("Attribute "+attribute.getName()+" "+attribute.getNameFormat());
                if (attribute.getName().equals("Email") && attribute.getNameFormat().equals("urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified")) {

                    List<Object> values = attribute.getAttributeValue();
                    logger.debug("Values.size="+values.size());

                    if (values.size() == 1) {

                        Object value = values.get(0);
                        logger.debug("value="+value);

                        if (value instanceof String stringValue) {

                            if (!stringValue.isBlank()) {

                                result = stringValue;
                                break outer;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    protected UserModel getExistingUserForEmail(AuthenticationFlowContext context, String email, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        UserModel existingUser;

        if (email != null) {
            existingUser = context.getSession().users().getUserByEmail(context.getRealm(), email);
            return existingUser;
        }

        return null;
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

}
