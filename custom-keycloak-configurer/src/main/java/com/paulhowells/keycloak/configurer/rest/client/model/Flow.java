package com.paulhowells.keycloak.configurer.rest.client.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Flow extends BaseModel {

    private String id;
    private String alias;
    private String description;
    private String providerId;
    private Boolean topLevel;
    private Boolean builtIn;

    @JsonIgnore
    private String realm;
    @JsonIgnore
    private int level;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthenticationExecution {

        private String authenticatorConfig;
        private String authenticator;
        private Boolean authenticatorFlow;
        private String requirement;
        private Long priority;
        private Boolean userSetupAllowed;
        private String flowAlias;

        public String getAuthenticatorConfig() {
            return authenticatorConfig;
        }

        public void setAuthenticatorConfig(String authenticatorConfig) {
            this.authenticatorConfig = authenticatorConfig;
        }

        public String getAuthenticator() {
            return authenticator;
        }

        public void setAuthenticator(String authenticator) {
            this.authenticator = authenticator;
        }

        public Boolean getAuthenticatorFlow() {
            return authenticatorFlow;
        }

        public void setAuthenticatorFlow(Boolean authenticatorFlow) {
            this.authenticatorFlow = authenticatorFlow;
        }

        public String getRequirement() {
            return requirement;
        }

        public void setRequirement(String requirement) {
            this.requirement = requirement;
        }

        public Long getPriority() {
            return priority;
        }

        public void setPriority(Long priority) {
            this.priority = priority;
        }

        public Boolean getUserSetupAllowed() {
            return userSetupAllowed;
        }

        public void setUserSetupAllowed(Boolean userSetupAllowed) {
            this.userSetupAllowed = userSetupAllowed;
        }

        public String getFlowAlias() {
            return flowAlias;
        }

        public void setFlowAlias(String flowAlias) {
            this.flowAlias = flowAlias;
        }
    }

    private List<AuthenticationExecution> authenticationExecutions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Boolean getTopLevel() {
        return topLevel;
    }

    public void setTopLevel(Boolean topLevel) {
        this.topLevel = topLevel;
    }

    public Boolean getBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(Boolean builtIn) {
        this.builtIn = builtIn;
    }

    public List<AuthenticationExecution> getAuthenticationExecutions() {
        return authenticationExecutions;
    }

    public void setAuthenticationExecutions(List<AuthenticationExecution> authenticationExecutions) {
        this.authenticationExecutions = authenticationExecutions;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
