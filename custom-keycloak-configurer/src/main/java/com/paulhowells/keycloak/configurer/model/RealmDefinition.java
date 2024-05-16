package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import org.slf4j.Logger;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(converter= RealmDefinition.PostConstruct.class)
public class RealmDefinition extends BaseDefinition {

    private String realmName;
    private String defaultBrowserFlowAlias;
    private String defaultRegistrationFlowAlias;
    private String defaultDirectGrantFlowAlias;
    private String defaultResetCredentialsFlowAlias;
    private String defaultClientAuthenticationFlowAlias;
    private String defaultFirstBrokerLoginFlowAlias;
    private Boolean enabled = Boolean.FALSE;
    private GeneralSettings generalSettings;
    private LoginSettings loginSettings;
    private EmailSettings emailSettings;
    private EventSettings eventSettings;
    private SessionSettings sessionSettings;
    private TokenSettings tokenSettings;
    private UserProfile userProfile = new UserProfile();

    public static class DefaultRole extends BaseDefinition {
        private String clientId;
        @JsonIgnore
        private String name;
        @JsonIgnore
        private String id;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
    private Map<String, DefaultRole> defaultRoles = new HashMap<>();
    private Map<String, ScopeDefinition> scopes = new HashMap<>();
    private Map<String, RoleDefinition> roles = new HashMap<>();
    private PasswordPolicy passwordPolicy;
    private GoogleIdentityProvider googleIdentityProvider;

    @Override
    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = super.isUnchanged(o, parentName, logger);

        if (result){

            RealmDefinition other = (RealmDefinition) o;

            result = isUnchanged(this.realmName, other.realmName, parentName, "realmName", logger) &
                    isUnchanged(this.defaultBrowserFlowAlias, other.defaultBrowserFlowAlias, parentName, "defaultBrowserFlowAlias", logger) &
                    isUnchanged(this.defaultRegistrationFlowAlias, other.defaultRegistrationFlowAlias, parentName, "defaultRegistrationFlowAlias", logger) &
                    isUnchanged(this.defaultDirectGrantFlowAlias, other.defaultDirectGrantFlowAlias, parentName, "defaultDirectGrantFlowAlias", logger) &
                    isUnchanged(this.defaultResetCredentialsFlowAlias, other.defaultResetCredentialsFlowAlias, parentName, "defaultResetCredentialsFlowAlias", logger) &
                    isUnchanged(this.defaultClientAuthenticationFlowAlias, other.defaultClientAuthenticationFlowAlias, parentName, "defaultClientAuthenticationFlowAlias", logger) &
                    isUnchanged(this.defaultFirstBrokerLoginFlowAlias, other.defaultFirstBrokerLoginFlowAlias, parentName, "defaultFirstBrokerLoginFlowAlias", logger) &
                    isUnchanged(this.enabled, other.enabled, parentName, "enabled", logger);
        }

        return result;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public String getDefaultBrowserFlowAlias() {
        return defaultBrowserFlowAlias;
    }

    public void setDefaultBrowserFlowAlias(String defaultBrowserFlowAlias) {
        this.defaultBrowserFlowAlias = defaultBrowserFlowAlias;
    }

    public String getDefaultRegistrationFlowAlias() {
        return defaultRegistrationFlowAlias;
    }

    public void setDefaultRegistrationFlowAlias(String defaultRegistrationFlowAlias) {
        this.defaultRegistrationFlowAlias = defaultRegistrationFlowAlias;
    }

    public String getDefaultDirectGrantFlowAlias() {
        return defaultDirectGrantFlowAlias;
    }

    public void setDefaultDirectGrantFlowAlias(String defaultDirectGrantFlowAlias) {
        this.defaultDirectGrantFlowAlias = defaultDirectGrantFlowAlias;
    }

    public String getDefaultResetCredentialsFlowAlias() {
        return defaultResetCredentialsFlowAlias;
    }

    public void setDefaultResetCredentialsFlowAlias(String defaultResetCredentialsFlowAlias) {
        this.defaultResetCredentialsFlowAlias = defaultResetCredentialsFlowAlias;
    }

    public String getDefaultClientAuthenticationFlowAlias() {
        return defaultClientAuthenticationFlowAlias;
    }

    public void setDefaultClientAuthenticationFlowAlias(String defaultClientAuthenticationFlowAlias) {
        this.defaultClientAuthenticationFlowAlias = defaultClientAuthenticationFlowAlias;
    }

    public String getDefaultFirstBrokerLoginFlowAlias() {
        return defaultFirstBrokerLoginFlowAlias;
    }

    public void setDefaultFirstBrokerLoginFlowAlias(String defaultFirstBrokerLoginFlowAlias) {
        this.defaultFirstBrokerLoginFlowAlias = defaultFirstBrokerLoginFlowAlias;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public GeneralSettings getGeneralSettings() {
        return generalSettings;
    }

    public void setGeneralSettings(GeneralSettings generalSettings) {
        this.generalSettings = generalSettings;
    }

    public LoginSettings getLoginSettings() {
        return loginSettings;
    }

    public void setLoginSettings(LoginSettings loginSettings) {
        this.loginSettings = loginSettings;
    }

    public EmailSettings getEmailSettings() {
        return emailSettings;
    }

    public void setEmailSettings(EmailSettings emailSettings) {
        this.emailSettings = emailSettings;
    }

    public EventSettings getEventSettings() {
        return eventSettings;
    }

    public void setEventSettings(EventSettings eventSettings) {
        this.eventSettings = eventSettings;
    }

    public SessionSettings getSessionSettings() {
        return sessionSettings;
    }

    public void setSessionSettings(SessionSettings sessionSettings) {
        this.sessionSettings = sessionSettings;
    }

    public TokenSettings getTokenSettings() {
        return tokenSettings;
    }

    public void setTokenSettings(TokenSettings tokenSettings) {
        this.tokenSettings = tokenSettings;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }

    public Map<String, DefaultRole> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(Map<String, DefaultRole> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public Map<String, ScopeDefinition> getScopes() {
        return scopes;
    }

    public void setScopes(Map<String, ScopeDefinition> scopes) {
        this.scopes = scopes;
    }

    public Map<String, RoleDefinition> getRoles() {
        return roles;
    }

    public void setRoles(Map<String, RoleDefinition> roles) {
        this.roles = roles;
    }

    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(PasswordPolicy passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public GoogleIdentityProvider getGoogleIdentityProvider() {
        return googleIdentityProvider;
    }

    public void setGoogleIdentityProvider(GoogleIdentityProvider googleIdentityProvider) {
        this.googleIdentityProvider = googleIdentityProvider;
    }

    static class PostConstruct extends StdConverter<RealmDefinition, RealmDefinition> {

        @Override
        public RealmDefinition convert(RealmDefinition o) {

            // Set the name on the default roles
            for(String name:o.defaultRoles.keySet()) {

                DefaultRole role = o.defaultRoles.get(name);

                role.setName(name);
            }

            // Set the name on the roles
            for(String name:o.roles.keySet()) {

                RoleDefinition role = o.roles.get(name);

                role.setName(name);
            }

            // Set the name on the roles
            for(String name:o.scopes.keySet()) {

                ScopeDefinition scope = o.scopes.get(name);

                scope.setName(name);
            }

            return o;
        }
    }
}
