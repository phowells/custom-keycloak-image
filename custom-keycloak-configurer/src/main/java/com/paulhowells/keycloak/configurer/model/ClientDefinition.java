package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import org.slf4j.Logger;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(converter= ClientDefinition.PostConstruct.class)
public class ClientDefinition extends BaseDefinition {

    private String realmName;
    private String clientId;
    private String clientSecret = null;
    private Boolean enabled = Boolean.FALSE;

    public static class GrantTypes extends BaseDefinition {

        public static class AuthorizationCode extends BaseDefinition {

            private Boolean enabled = Boolean.FALSE;
            private List<String> postLoginRedirectUris = new ArrayList<>();
            private List<String> webOrigins = new ArrayList<>();
            private List<String> postLogoutRedirectUris = new ArrayList<>();
            private String flowOverride;

            public Boolean getEnabled() {
                return enabled;
            }

            public void setEnabled(Boolean enabled) {
                this.enabled = enabled;
            }

            public List<String> getPostLoginRedirectUris() {
                return postLoginRedirectUris;
            }

            public void setPostLoginRedirectUris(List<String> postLoginRedirectUris) {
                this.postLoginRedirectUris = postLoginRedirectUris;
            }

            public List<String> getWebOrigins() {
                return webOrigins;
            }

            public void setWebOrigins(List<String> webOrigins) {
                this.webOrigins = webOrigins;
            }

            public List<String> getPostLogoutRedirectUris() {
                return postLogoutRedirectUris;
            }

            public void setPostLogoutRedirectUris(List<String> postLogoutRedirectUris) {
                this.postLogoutRedirectUris = postLogoutRedirectUris;
            }

            public String getFlowOverride() {
                return flowOverride;
            }

            public void setFlowOverride(String flowOverride) {
                this.flowOverride = flowOverride;
            }

            @Override
            public boolean isUnchanged(Object o, String parentName, Logger logger) {
                boolean result = super.isUnchanged(o, parentName, logger);

                if (result){

                    AuthorizationCode other = (AuthorizationCode) o;

                    result = isUnchanged(this.enabled, other.enabled, parentName, "enabled", logger) &
                            isUnchanged(this.postLoginRedirectUris, other.postLoginRedirectUris, parentName, "postLoginRedirectUris", logger) &
                            isUnchanged(this.webOrigins, other.webOrigins, parentName, "webOrigins", logger) &
                            isUnchanged(this.postLogoutRedirectUris, other.postLogoutRedirectUris, parentName, "postLogoutRedirectUris", logger) &
                            isUnchanged(this.flowOverride, other.flowOverride, parentName, "flowOverride", logger);
                }

                return result;
            }
        }

        private AuthorizationCode authorizationCode = new AuthorizationCode();

        public static class Password extends BaseDefinition {

            private Boolean enabled = Boolean.FALSE;
            private String flowOverride;

            public Boolean getEnabled() {
                return enabled;
            }

            public void setEnabled(Boolean enabled) {
                this.enabled = enabled;
            }

            public String getFlowOverride() {
                return flowOverride;
            }

            public void setFlowOverride(String flowOverride) {
                this.flowOverride = flowOverride;
            }

            @Override
            public boolean isUnchanged(Object o, String parentName, Logger logger) {
                boolean result = super.isUnchanged(o, parentName, logger);

                if (result){

                    Password other = (Password) o;

                    result = isUnchanged(this.enabled, other.enabled, parentName, "enabled", logger) &
                            isUnchanged(this.flowOverride, other.flowOverride, parentName, "flowOverride", logger);
                }

                return result;
            }
        }

        private Password password = new Password();

        public static class ClientCredentials extends BaseDefinition {

            private Boolean enabled = Boolean.FALSE;

            public Boolean getEnabled() {
                return enabled;
            }

            public void setEnabled(Boolean enabled) {
                this.enabled = enabled;
            }

            @Override
            public boolean isUnchanged(Object o, String parentName, Logger logger) {
                boolean result = super.isUnchanged(o, parentName, logger);

                if (result){

                    ClientCredentials other = (ClientCredentials) o;

                    result = isUnchanged(this.enabled, other.enabled, parentName, "enabled", logger);
                }

                return result;
            }
        }

        private ClientCredentials clientCredentials = new ClientCredentials();

        public AuthorizationCode getAuthorizationCode() {
            return authorizationCode;
        }

        public void setAuthorizationCode(AuthorizationCode authorizationCode) {
            this.authorizationCode = authorizationCode;
        }

        public Password getPassword() {
            return password;
        }

        public void setPassword(Password password) {
            this.password = password;
        }

        public ClientCredentials getClientCredentials() {
            return clientCredentials;
        }

        public void setClientCredentials(ClientCredentials clientCredentials) {
            this.clientCredentials = clientCredentials;
        }

        @Override
        public boolean isUnchanged(Object o, String parentName, Logger logger) {
            boolean result = super.isUnchanged(o, parentName, logger);

            if (result){

                GrantTypes other = (GrantTypes) o;

                result = isUnchanged(this.authorizationCode, other.authorizationCode, parentName, "authorizationCode", logger) &
                        isUnchanged(this.password, other.password, parentName, "password", logger) &
                        isUnchanged(this.clientCredentials, other.clientCredentials, parentName, "clientCredentials", logger);
            }

            return result;
        }
    }

    private GrantTypes grantTypes = new GrantTypes();

    public static class Scope extends BaseDefinition {
        private String type;

        @JsonIgnore
        private String id;

        @JsonIgnore
        private String name;

        @Override
        public boolean isUnchanged(Object o, String parentName, Logger logger) {
            boolean result = super.isUnchanged(o, parentName, logger);

            if (result){

                Scope other = (Scope) o;

                result = isUnchanged(this.type, other.type, parentName, "type", logger);
            }

            return result;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private Map<String, Scope> scopes = new HashMap<>();

    public static class AdvancedSettings extends BaseDefinition {

        private Long accessTokenLifespanSeconds;
        private Long clientSessionIdleTimeoutSeconds;
        private Long clientSessionMaxLifespanSeconds;
        private Long clientOfflineSessionIdleTimeoutSeconds;
        private Long clientOfflineSessionMaxLifespanSeconds;

        @Override
        public boolean isUnchanged(Object o, String parentName, Logger logger) {
            boolean result = super.isUnchanged(o, parentName, logger);

            if (result){

                AdvancedSettings other = (AdvancedSettings) o;

                result = isUnchanged(this.accessTokenLifespanSeconds, other.accessTokenLifespanSeconds, parentName, "accessTokenLifespanSeconds", logger) &
                        isUnchanged(this.clientSessionIdleTimeoutSeconds, other.clientSessionIdleTimeoutSeconds, parentName, "clientSessionIdleTimeoutSeconds", logger) &
                        isUnchanged(this.clientSessionMaxLifespanSeconds, other.clientSessionMaxLifespanSeconds, parentName, "clientSessionMaxLifespanSeconds", logger) &
                        isUnchanged(this.clientOfflineSessionIdleTimeoutSeconds, other.clientOfflineSessionIdleTimeoutSeconds, parentName, "clientOfflineSessionIdleTimeoutSeconds", logger) &
                        isUnchanged(this.clientOfflineSessionMaxLifespanSeconds, other.clientOfflineSessionMaxLifespanSeconds, parentName, "clientOfflineSessionMaxLifespanSeconds", logger);
            }

            return result;
        }

        public Long getAccessTokenLifespanSeconds() {
            return accessTokenLifespanSeconds;
        }

        public void setAccessTokenLifespanSeconds(Long accessTokenLifespanSeconds) {
            this.accessTokenLifespanSeconds = accessTokenLifespanSeconds;
        }

        public Long getClientSessionIdleTimeoutSeconds() {
            return clientSessionIdleTimeoutSeconds;
        }

        public void setClientSessionIdleTimeoutSeconds(Long clientSessionIdleTimeoutSeconds) {
            this.clientSessionIdleTimeoutSeconds = clientSessionIdleTimeoutSeconds;
        }

        public Long getClientSessionMaxLifespanSeconds() {
            return clientSessionMaxLifespanSeconds;
        }

        public void setClientSessionMaxLifespanSeconds(Long clientSessionMaxLifespanSeconds) {
            this.clientSessionMaxLifespanSeconds = clientSessionMaxLifespanSeconds;
        }

        public Long getClientOfflineSessionIdleTimeoutSeconds() {
            return clientOfflineSessionIdleTimeoutSeconds;
        }

        public void setClientOfflineSessionIdleTimeoutSeconds(Long clientOfflineSessionIdleTimeoutSeconds) {
            this.clientOfflineSessionIdleTimeoutSeconds = clientOfflineSessionIdleTimeoutSeconds;
        }

        public Long getClientOfflineSessionMaxLifespanSeconds() {
            return clientOfflineSessionMaxLifespanSeconds;
        }

        public void setClientOfflineSessionMaxLifespanSeconds(Long clientOfflineSessionMaxLifespanSeconds) {
            this.clientOfflineSessionMaxLifespanSeconds = clientOfflineSessionMaxLifespanSeconds;
        }
    }

    private AdvancedSettings advancedSettings = new AdvancedSettings();

    // Internal Keycloak identifier
    @JsonIgnore
    private String id;

    @Override
    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = super.isUnchanged(o, parentName, logger);

        if (result){

            ClientDefinition other = (ClientDefinition) o;

            result = isUnchanged(this.realmName, other.realmName, parentName, "realmName", logger) &
                    isUnchanged(this.clientId, other.clientId, parentName, "clientId", logger) &
                    isUnchanged(this.clientSecret, other.clientSecret, parentName, "clientSecret", true, logger) &
                    isUnchanged(this.enabled, other.enabled, parentName, "enabled", logger) &
                    isUnchanged(this.grantTypes, other.grantTypes, parentName, "grantTypes", logger) &
                    isUnchanged(this.advancedSettings, other.advancedSettings, parentName, "advancedSettings", logger);
        }

        return result;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public GrantTypes getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(GrantTypes grantTypes) {
        this.grantTypes = grantTypes;
    }

    public Map<String, Scope> getScopes() {
        return scopes;
    }

    public void setScopes(Map<String, Scope> scopes) {
        this.scopes = scopes;
    }

    public AdvancedSettings getAdvancedSettings() {
        return advancedSettings;
    }

    public void setAdvancedSettings(AdvancedSettings advancedSettings) {
        this.advancedSettings = advancedSettings;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    static class PostConstruct extends StdConverter<ClientDefinition, ClientDefinition> {

        @Override
        public ClientDefinition convert(ClientDefinition o) {

            // Set the name on the roles
            for(String name:o.scopes.keySet()) {

                Scope scope = o.scopes.get(name);

                scope.setName(name);
            }

            return o;
        }
    }
}
