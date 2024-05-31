package com.geotab.keycloak.events;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.common.util.StackUtil;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ResolvedLoggingEventListenerProvider implements EventListenerProvider {

    private final KeycloakSession session;
    private final Logger logger;
    private final Logger.Level successLevel;
    private final Logger.Level errorLevel;
    private final boolean sanitize;
    private final Character quotes;
    private final EventListenerTransaction tx = new EventListenerTransaction(this::logAdminEvent, this::logEvent);

    public ResolvedLoggingEventListenerProvider(KeycloakSession session, Logger logger,
                                                Logger.Level successLevel, Logger.Level errorLevel, Character quotes, boolean sanitize) {
        this.session = session;
        this.logger = logger;
        this.successLevel = successLevel;
        this.errorLevel = errorLevel;
        this.sanitize = sanitize;
        this.quotes = quotes;
        this.session.getTransactionManager().enlistAfterCompletion(tx);
    }

    @Override
    public void onEvent(Event event) {
        tx.addEvent(event);
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        tx.addAdminEvent(adminEvent, includeRepresentation);
    }

    private void sanitize(StringBuilder sb, String str) {
        if (quotes != null) {
            sb.append(quotes);
        }
        if (sanitize) {
            str = StringUtil.sanitizeSpacesAndQuotes(str, quotes);
        }
        sb.append(str);
        if (quotes != null) {
            sb.append(quotes);
        }
    }

    private void logEvent(Event event) {
        Logger.Level level = event.getError() != null ? errorLevel : successLevel;

        if (logger.isEnabled(level)) {

            String[] resolved = resolve(event.getRealmId(), event.getUserId());

            String realmName = resolved[0];
            String userRealm = resolved[1];
            String username = resolved[2];

            StringBuilder sb = new StringBuilder();

            sb.append("type=");
            sanitize(sb, event.getType().toString());
            sb.append(", realmId=");
            sanitize(sb, event.getRealmId());
            sb.append(", realm=");
            sanitize(sb, realmName);
            sb.append(", clientId=");
            sanitize(sb, event.getClientId());
            sb.append(", userId=");
            sanitize(sb, event.getUserId());
            sb.append(", ipAddress=");
            sanitize(sb, event.getIpAddress());

            if (event.getError() != null) {
                sb.append(", error=");
                sanitize(sb, event.getError());
            }

            if (event.getDetails() != null) {

                Map<String, String> details = new HashMap<>(event.getDetails());
                details.put("userRealm", userRealm);
                details.put("username", username);

                for (Map.Entry<String, String> e : event.getDetails().entrySet()) {
                    sb.append(", ");
                    sb.append(StringUtil.sanitizeSpacesAndQuotes(e.getKey(), null));
                    sb.append("=");
                    sanitize(sb, e.getValue());
                }
            }

            AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();
            if(authSession!=null) {
                sb.append(", authSessionParentId=");
                sanitize(sb, authSession.getParentSession().getId());
                sb.append(", authSessionTabId=");
                sanitize(sb, authSession.getTabId());
            }

            if(logger.isTraceEnabled()) {
                setKeycloakContext(sb);

                if (StackUtil.isShortStackTraceEnabled()) {
                    sb.append(", stackTrace=").append(StackUtil.getShortStackTrace());
                }
            }

            logger.log(logger.isTraceEnabled() ? Logger.Level.TRACE : level, sb.toString());
        }
    }

    private void logAdminEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        Logger.Level level = adminEvent.getError() != null ? errorLevel : successLevel;

        if (logger.isEnabled(level)) {

            String[] resolved = resolve(adminEvent.getRealmId(), adminEvent.getAuthDetails().getUserId());

            String realmName = resolved[0];
            String userRealm = resolved[1];
            String username = resolved[2];

            StringBuilder sb = new StringBuilder();

            sb.append("operationType=");
            sanitize(sb, adminEvent.getOperationType().toString());
            sb.append(", realmId=");
            sanitize(sb, adminEvent.getAuthDetails().getRealmId());
            sb.append(", realm=");
            sanitize(sb, realmName);

            sb.append(", clientId=");
            sanitize(sb, adminEvent.getAuthDetails().getClientId());
            sb.append(", userId=");
            sanitize(sb, adminEvent.getAuthDetails().getUserId());
            sb.append(", userRealm=");
            sanitize(sb, userRealm);
            sb.append(", username=");
            sanitize(sb, username);

            sb.append(", ipAddress=");
            sanitize(sb, adminEvent.getAuthDetails().getIpAddress());
            sb.append(", resourceType=");
            sanitize(sb, adminEvent.getResourceTypeAsString());
            sb.append(", resourcePath=");
            sanitize(sb, adminEvent.getResourcePath());

            if (adminEvent.getError() != null) {
                sb.append(", error=");
                sanitize(sb, adminEvent.getError());
            }

            if(logger.isTraceEnabled()) {
                setKeycloakContext(sb);
            }

            logger.log(logger.isTraceEnabled() ? Logger.Level.TRACE : level, sb.toString());
        }
    }

    private String[] resolve(String realmId, String userId) {

        AtomicReference<String> realmName = new AtomicReference<>();
        AtomicReference<String> userRealm = new AtomicReference<>();
        AtomicReference<String> username = new AtomicReference<>();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), s -> {
            RealmModel realm = s.realms().getRealm(realmId);

            if (realm != null) {
                realmName.set(realm.getName());
                userRealm.set(realm.getName());

                if (userId != null && !userId.isBlank()) {
                    UserModel user = s.users().getUserById(realm, userId);

                    if (user == null) {

                        RealmModel masterRealm = s.realms().getRealmByName("master");
                        userRealm.set(masterRealm.getName());

                        user = s.users().getUserById(masterRealm, userId);
                    }

                    if (user != null) {

                        username.set(user.getUsername());
                    }
                }
            }
        });

        return new String[] {realmName.get(), userRealm.get(), username.get()};
    }

    @Override
    public void close() {
    }

    private void setKeycloakContext(StringBuilder sb) {
        KeycloakContext context = session.getContext();
        UriInfo uriInfo = context.getUri();
        HttpHeaders headers = context.getRequestHeaders();
        if (uriInfo != null) {
            sb.append(", requestUri=");
            sanitize(sb, uriInfo.getRequestUri().toString());
        }

        if (headers != null) {
            sb.append(", cookies=[");
            boolean f = true;
            for (Map.Entry<String, Cookie> e : headers.getCookies().entrySet()) {
                if (f) {
                    f = false;
                } else {
                    sb.append(", ");
                }
                sb.append(StringUtil.sanitizeSpacesAndQuotes(e.getValue().toString(), null));
            }
            sb.append("]");
        }
    }
}
