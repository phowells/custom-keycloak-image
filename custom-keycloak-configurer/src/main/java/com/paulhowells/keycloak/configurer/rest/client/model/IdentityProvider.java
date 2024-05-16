package com.paulhowells.keycloak.configurer.rest.client.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdentityProvider extends BaseModel {

    public static final String GOOGLE_IDP_PROVIDER_ID = "google";

    public static final String GOOGLE_IDP_ALIAS = "google";

    private String alias;

    private String internalId;

    private String providerId;

    private Boolean enabled;

    private String updateProfileFirstLoginMode;

    private Boolean trustEmail;

    private Boolean storeToken;

    private Boolean addReadTokenRoleOnCreate;

    private Boolean authenticateByDefault;

    private Boolean linkOnly;

    private String firstBrokerLoginFlowAlias;

    private String postBrokerLoginFlowAlias;

    public static class Config {
        private String clientId;
        private String clientSecret;
        private String hideOnLoginPage;
        private String hostedDomain;
        private String offlineAccess;
        private String acceptsPromptNoneForwardFromClient;
        private String defaultScope;
        private String disableUserInfo;
        private String filteredByClaim;
        private String claimFilterName;
        private String claimFilterValue;
        private String syncMode;
        private String guiOrder;
        private String userIp;
        private String managedBy;

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

        public String getHideOnLoginPage() {
            return hideOnLoginPage;
        }

        public void setHideOnLoginPage(String hideOnLoginPage) {
            this.hideOnLoginPage = hideOnLoginPage;
        }

        public String getHostedDomain() {
            return hostedDomain;
        }

        public void setHostedDomain(String hostedDomain) {
            this.hostedDomain = hostedDomain;
        }

        public String getOfflineAccess() {
            return offlineAccess;
        }

        public void setOfflineAccess(String offlineAccess) {
            this.offlineAccess = offlineAccess;
        }

        public String getAcceptsPromptNoneForwardFromClient() {
            return acceptsPromptNoneForwardFromClient;
        }

        public void setAcceptsPromptNoneForwardFromClient(String acceptsPromptNoneForwardFromClient) {
            this.acceptsPromptNoneForwardFromClient = acceptsPromptNoneForwardFromClient;
        }

        public String getDefaultScope() {
            return defaultScope;
        }

        public void setDefaultScope(String defaultScope) {
            this.defaultScope = defaultScope;
        }

        public String getDisableUserInfo() {
            return disableUserInfo;
        }

        public void setDisableUserInfo(String disableUserInfo) {
            this.disableUserInfo = disableUserInfo;
        }

        public String getFilteredByClaim() {
            return filteredByClaim;
        }

        public void setFilteredByClaim(String filteredByClaim) {
            this.filteredByClaim = filteredByClaim;
        }

        public String getClaimFilterName() {
            return claimFilterName;
        }

        public void setClaimFilterName(String claimFilterName) {
            this.claimFilterName = claimFilterName;
        }

        public String getClaimFilterValue() {
            return claimFilterValue;
        }

        public void setClaimFilterValue(String claimFilterValue) {
            this.claimFilterValue = claimFilterValue;
        }

        public String getSyncMode() {
            return syncMode;
        }

        public void setSyncMode(String syncMode) {
            this.syncMode = syncMode;
        }

        public String getGuiOrder() {
            return guiOrder;
        }

        public void setGuiOrder(String guiOrder) {
            this.guiOrder = guiOrder;
        }

        public String getUserIp() {
            return userIp;
        }

        public void setUserIp(String userIp) {
            this.userIp = userIp;
        }

        public String getManagedBy() {
            return managedBy;
        }

        public void setManagedBy(String managedBy) {
            this.managedBy = managedBy;
        }
    }

    private Config config = new Config();

    @JsonIgnore
    private String realm;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getUpdateProfileFirstLoginMode() {
        return updateProfileFirstLoginMode;
    }

    public void setUpdateProfileFirstLoginMode(String updateProfileFirstLoginMode) {
        this.updateProfileFirstLoginMode = updateProfileFirstLoginMode;
    }

    public Boolean getTrustEmail() {
        return trustEmail;
    }

    public void setTrustEmail(Boolean trustEmail) {
        this.trustEmail = trustEmail;
    }

    public Boolean getStoreToken() {
        return storeToken;
    }

    public void setStoreToken(Boolean storeToken) {
        this.storeToken = storeToken;
    }

    public Boolean getAddReadTokenRoleOnCreate() {
        return addReadTokenRoleOnCreate;
    }

    public void setAddReadTokenRoleOnCreate(Boolean addReadTokenRoleOnCreate) {
        this.addReadTokenRoleOnCreate = addReadTokenRoleOnCreate;
    }

    public Boolean getAuthenticateByDefault() {
        return authenticateByDefault;
    }

    public void setAuthenticateByDefault(Boolean authenticateByDefault) {
        this.authenticateByDefault = authenticateByDefault;
    }

    public Boolean getLinkOnly() {
        return linkOnly;
    }

    public void setLinkOnly(Boolean linkOnly) {
        this.linkOnly = linkOnly;
    }

    public String getFirstBrokerLoginFlowAlias() {
        return firstBrokerLoginFlowAlias;
    }

    public void setFirstBrokerLoginFlowAlias(String firstBrokerLoginFlowAlias) {
        this.firstBrokerLoginFlowAlias = firstBrokerLoginFlowAlias;
    }

    public String getPostBrokerLoginFlowAlias() {
        return postBrokerLoginFlowAlias;
    }

    public void setPostBrokerLoginFlowAlias(String postBrokerLoginFlowAlias) {
        this.postBrokerLoginFlowAlias = postBrokerLoginFlowAlias;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }
}