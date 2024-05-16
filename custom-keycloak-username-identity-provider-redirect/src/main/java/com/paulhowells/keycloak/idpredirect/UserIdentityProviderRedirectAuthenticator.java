package com.paulhowells.keycloak.idpredirect;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Paul Howells
 */
public class UserIdentityProviderRedirectAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(UserIdentityProviderRedirectAuthenticator.class);

    protected static final String ACCEPTS_PROMPT_NONE = "acceptsPromptNoneForwardFromClient";

    protected static final String IDENTITY_PROVIDER_USER_ATTRIBUTE_NAME = "identity-provider-name";

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        String identityProviderName = context.getUser().getFirstAttribute(IDENTITY_PROVIDER_USER_ATTRIBUTE_NAME);

        if (identityProviderName == null || identityProviderName.isBlank()) {

            LOG.warnf("No default provider set or %s query parameter provided", AdapterConstants.KC_IDP_HINT);
            context.attempted();
        } else {
            LOG.warnf("Redirecting: %s set to %s", IDENTITY_PROVIDER_USER_ATTRIBUTE_NAME, identityProviderName);
            redirect(context, identityProviderName);
        }
    }

    private void redirect(AuthenticationFlowContext context, String providerId) {
        Optional<IdentityProviderModel> idp = context.getRealm().getIdentityProvidersStream()
                .filter(IdentityProviderModel::isEnabled)
                .filter(identityProvider -> Objects.equals(providerId, identityProvider.getAlias()))
                .findFirst();
        if (idp.isPresent()) {
            String accessCode = new ClientSessionCode<>(context.getSession(), context.getRealm(), context.getAuthenticationSession()).getOrGenerateCode();
            String clientId = context.getAuthenticationSession().getClient().getClientId();
            String tabId = context.getAuthenticationSession().getTabId();
            URI location = Urls.identityProviderAuthnRequest(context.getUriInfo().getBaseUri(), providerId, context.getRealm().getName(), accessCode, clientId, tabId);
            Response response = Response.seeOther(location)
                    .build();
            // will forward the request to the IDP with prompt=none if the IDP accepts forwards with prompt=none.
            if ("none".equals(context.getAuthenticationSession().getClientNote(OIDCLoginProtocol.PROMPT_PARAM)) &&
                    Boolean.parseBoolean(idp.get().getConfig().get(ACCEPTS_PROMPT_NONE))) {
                context.getAuthenticationSession().setAuthNote(AuthenticationProcessor.FORWARDED_PASSIVE_LOGIN, "true");
            }
            LOG.warnf("Redirecting to %s", providerId);
            context.forceChallenge(response);
            return;
        }

        LOG.warnf("Provider not found or not enabled for realm %s", providerId);
        context.attempted();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
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

    @Override
    public void close() {
    }

}
