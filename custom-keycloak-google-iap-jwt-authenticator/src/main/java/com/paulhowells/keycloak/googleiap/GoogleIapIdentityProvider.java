package com.paulhowells.keycloak.googleiap;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.*;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GoogleIapIdentityProvider implements IdentityProvider<IdentityProviderModel> {

    private static final Logger logger = Logger.getLogger(GoogleIapIdentityProvider.class);

    private final KeycloakSession session;

    public GoogleIapIdentityProvider(KeycloakSession session) {
        logger.info("<GoogleIapIdentityProvider");

        this.session = session;

        logger.info(">GoogleIapIdentityProvider");
    }

    @Override
    public IdentityProviderModel getConfig() {
        logger.info("<getConfig");
        IdentityProviderModel result = new IdentityProviderModel();

        logger.info(">getConfig " + result);
        return result;
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession keycloakSession, RealmModel realmModel, BrokeredIdentityContext brokeredIdentityContext) {
        logger.info("<preprocessFederatedIdentity");

        logger.info(">preprocessFederatedIdentity");
    }

    @Override
    public void authenticationFinished(AuthenticationSessionModel authenticationSessionModel, BrokeredIdentityContext brokeredIdentityContext) {
        logger.info("<authenticationFinished");

        logger.info(">authenticationFinished");
    }

    @Override
    public void importNewUser(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel, BrokeredIdentityContext brokeredIdentityContext) {
        logger.info("<importNewUser");

        logger.info(">importNewUser");
    }

    @Override
    public void updateBrokeredUser(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel, BrokeredIdentityContext brokeredIdentityContext) {
        logger.info("<updateBrokeredUser");

        logger.info(">updateBrokeredUser");
    }

    @Override
    public Object callback(RealmModel realmModel, AuthenticationCallback authenticationCallback, EventBuilder eventBuilder) {
        logger.info("<callback");
        Object result = null;

        logger.info(">callback "+result);
        return result;
    }

    public static final String GOOGLE_IAP_JWT_ASSERTION_HEADER = "X-Goog-IAP-JWT-Assertion";

    @Override
    public Response performLogin(AuthenticationRequest authenticationRequest) {
        logger.info("<performLogin");
        Response result = null;

        MultivaluedMap<String, String> headers = authenticationRequest.getHttpRequest().getHttpHeaders().getRequestHeaders();
        logger.info("Request Headers:");
        for (String key:headers.keySet()) {

            List<String> values = headers.get(key);

            for (String value:values) {
                logger.infof("%s=%s", key, value);
            }
        }

        // search for the authentication header X-Goog-IAP-JWT-Assertion
        String iapJwtHeader = authenticationRequest.getHttpRequest().getHttpHeaders().getHeaderString(GOOGLE_IAP_JWT_ASSERTION_HEADER);

        if (iapJwtHeader == null || iapJwtHeader.isBlank()) {

            // if he header is not present returns 401
            result = Response.status(401).build();
        } else {
            logger.infof("jwt=%s", iapJwtHeader);

            SignedJWT signedJWT = null;
            try {
                signedJWT = SignedJWT.parse(iapJwtHeader);

            } catch (ParseException e) {
                logger.warn("Failed to parse Google IAP JWT %s", e);
                result = Response.status(401).build();
            }

            JWTClaimsSet jwtClaimSet = null;
            if (signedJWT != null) {
                try {
                    jwtClaimSet = signedJWT.getJWTClaimsSet();
                } catch (ParseException e) {
                    logger.warn("Failed to parse Google IAP JWT %s", e);
                    result = Response.status(401).build();
                }
            }

            if (jwtClaimSet != null) {

                log(jwtClaimSet);

                // if the header exists validates the following:
                // token signature
                // token issuer - configurable value, defaults to https://cloud.google.com/iap
                // token audience - configurable value
                // token is not expired
                // token is not emitted in the future
                // if one of the above check fails returns 403

//                String algorithm = null;
//                try {
//                    algorithm = jwtClaimSet.getStringClaim("alg");
//                } catch (ParseException e) {
//                    logger.warn("Failed to parse Google IAP JWT %s", e);
//                    result = Response.status(401).build();
//                }
//
//                if (algorithm != null) {
//
//                    logger.infof("alg=%s", algorithm);
//
//                    if (!"ES256".equals(algorithm)) {
//
//                        logger.warnf("Invalid signature algorithm %s", algorithm);
//                        result = Response.status(401).build();
//                    } else {
//
//
//                    }
//
//                    JWSVerifier verifier = new Ed25519Verifier(publicJWK);
//                    assertTrue(signedJWT.verify(verifier));
//                }

                BrokeredIdentityContext federatedIdentity = null;
                try {
                    federatedIdentity = getFederatedIdentity(jwtClaimSet);
                } catch (ParseException e) {
                    logger.warn("Failed to parse Google IAP JWT %s", e);
                    result = Response.status(401).build();
                }

                if (federatedIdentity != null) {

                    federatedIdentity.setIdpConfig(this.getConfig());
                    federatedIdentity.setIdp(this);
                    federatedIdentity.setAuthenticationSession(authenticationRequest.getAuthenticationSession());

                    IdentityBrokerService service = new IdentityBrokerService(this.session);

                    result = service.authenticated(federatedIdentity);
                }
            }
        }

        logger.info(">performLogin "+result);
        return result;
    }

    private void log(JWTClaimsSet jwtClaimSet) {
        logger.info("Token Claims");

        Map<String, Object> claims = jwtClaimSet.getClaims();

        log(claims);
    }

    private void log(Map<?, ?> claims) {

        for (Object key:claims.keySet()) {

            Object value = claims.get(key);

            if (value instanceof Collection<?> list) {

                logger.infof("%s:");
                for (Object item:list) {
                    logger.infof(" - %s", item);
                }

            } else if (value instanceof Map<?,?> map) {
                logger.infof("%s: START MAP");

                log(map);

                logger.infof("%s: END MAP");
            } else {
                logger.infof("%s: %s", key, value);
            }
        }
    }

    public BrokeredIdentityContext getFederatedIdentity(JWTClaimsSet idToken) throws ParseException {
        logger.info("<getFederatedIdentity");

        String id = idToken.getSubject();
        BrokeredIdentityContext identity = new BrokeredIdentityContext(id);
        String name = idToken.getStringClaim("name");
        String givenName = idToken.getStringClaim("given_name");
        String familyName = idToken.getStringClaim("family_name");
        String preferredUsername = idToken.getStringClaim("preferred_username");
        String email = idToken.getStringClaim("email");

        identity.getContextData().put("VALIDATED_ID_TOKEN", idToken);
        identity.setId(id);
        if (givenName != null) {
            identity.setFirstName(givenName);
        }

        if (familyName != null) {
            identity.setLastName(familyName);
        }

        if (givenName == null && familyName == null) {
            identity.setName(name);
        }

        identity.setEmail(email);
        String var10001 = this.getConfig().getAlias();
        identity.setBrokerUserId(var10001 + "." + id);
        if (preferredUsername == null) {
            preferredUsername = email;
        }

        if (preferredUsername == null) {
            preferredUsername = id;
        }

        identity.setUsername(preferredUsername);

        logger.info(">getFederatedIdentity");
        return identity;
    }

    @Override
    public Response retrieveToken(KeycloakSession keycloakSession, FederatedIdentityModel federatedIdentityModel) {
        logger.info("<retrieveToken");
        Response result = null;

        logger.info(">retrieveToken "+result);
        return result;
    }

    @Override
    public void backchannelLogout(KeycloakSession keycloakSession, UserSessionModel userSessionModel, UriInfo uriInfo, RealmModel realmModel) {
        logger.info("<backchannelLogout");

        logger.info(">backchannelLogout");
    }

    @Override
    public Response keycloakInitiatedBrowserLogout(KeycloakSession keycloakSession, UserSessionModel userSessionModel, UriInfo uriInfo, RealmModel realmModel) {
        logger.info("<keycloakInitiatedBrowserLogout");
        Response result = null;

        logger.info(">keycloakInitiatedBrowserLogout "+result);
        return result;
    }

    @Override
    public Response export(UriInfo uriInfo, RealmModel realmModel, String s) {
        logger.info("<export");
        Response result = null;

        logger.info(">export "+result);
        return result;
    }

    @Override
    public IdentityProviderDataMarshaller getMarshaller() {
        logger.info("<getMarshaller");
        IdentityProviderDataMarshaller result = null;

        logger.info(">getMarshaller "+result);
        return result;
    }

    @Override
    public void close() {

    }
}
