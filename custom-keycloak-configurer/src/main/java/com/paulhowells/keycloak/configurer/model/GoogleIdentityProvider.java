package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GoogleIdentityProvider extends BaseDefinition {

    private String clientId;
    private String clientSecret;
    private Long displayOrder;
    private List<String> hostedDomains = new ArrayList<>();
    private Boolean useUserIpParam;
    private Boolean requestRefreshToken;
    private List<String> scopes = new ArrayList<>();
    private Boolean storeTokens;
    private Boolean acceptsPromptNoneForward;
    private Boolean disableUserInfo;
    private Boolean trustEmail;
    private Boolean accountLinkingOnly;
    private Boolean hideOnLoginPage;
    private Boolean enabled;
    private List<IdentityProviderMapperDefinition> mappers = new ArrayList<>();

    public static class EssentialClaim extends BaseDefinition {
        private Boolean enabled = Boolean.FALSE;
        private String claimName;
        private String regex;
        @Override
        public boolean isUnchanged(Object o, String parentName, Logger logger) {
            boolean result = super.isUnchanged(o, parentName, logger);

            if (result){

                EssentialClaim other = (EssentialClaim) o;

                result = isUnchanged(this.enabled, other.enabled, parentName, "enabled", logger) &
                        isUnchanged(this.claimName, other.claimName, parentName, "claimName", logger) &
                        isUnchanged(this.regex, other.regex, parentName, "regex", logger);
            }

            return result;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getClaimName() {
            return claimName;
        }

        public void setClaimName(String claimName) {
            this.claimName = claimName;
        }

        public String getRegex() {
            return regex;
        }

        public void setRegex(String regex) {
            this.regex = regex;
        }
    }
    private EssentialClaim essentialClaim = new EssentialClaim();

    private String firstLoginFlowAlias;
    private String postLoginFlowAlias;
    private String syncMode;

    @Override
    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = super.isUnchanged(o, parentName, logger);

        if (result){

            GoogleIdentityProvider other = (GoogleIdentityProvider) o;

            result = isUnchanged(this.clientId, other.clientId, parentName, "clientId", logger) &
                    isUnchanged(this.clientSecret, other.clientSecret, parentName, "clientSecret", true, logger) &
                    isUnchanged(this.displayOrder, other.displayOrder, parentName, "displayOrder", logger) &
                    isUnchanged(this.hostedDomains, other.hostedDomains, parentName, "hostedDomains", logger) &
                    isUnchanged(this.useUserIpParam, other.useUserIpParam, parentName, "useUserIpParam", logger) &
                    isUnchanged(this.requestRefreshToken, other.requestRefreshToken, parentName, "requestRefreshToken", logger) &
                    isUnchanged(this.scopes, other.scopes, parentName, "scopes", logger) &
                    isUnchanged(this.storeTokens, other.storeTokens, parentName, "storeTokens", logger) &
                    isUnchanged(this.acceptsPromptNoneForward, other.acceptsPromptNoneForward, parentName, "acceptsPromptNoneForward", logger) &
                    isUnchanged(this.acceptsPromptNoneForward, other.acceptsPromptNoneForward, parentName, "acceptsPromptNoneForward", logger) &
                    isUnchanged(this.disableUserInfo, other.disableUserInfo, parentName, "disableUserInfo", logger) &
                    isUnchanged(this.trustEmail, other.trustEmail, parentName, "trustEmail", logger) &
                    isUnchanged(this.accountLinkingOnly, other.accountLinkingOnly, parentName, "accountLinkingOnly", logger) &
                    isUnchanged(this.hideOnLoginPage, other.hideOnLoginPage, parentName, "hideOnLoginPage", logger) &
                    isUnchanged(this.essentialClaim, other.essentialClaim, parentName, "essentialClaim", logger) &
                    isUnchanged(this.firstLoginFlowAlias, other.firstLoginFlowAlias, parentName, "firstLoginFlowAlias", logger) &
                    isUnchanged(this.postLoginFlowAlias, other.postLoginFlowAlias, parentName, "postLoginFlowAlias", logger) &
                    isUnchanged(this.syncMode, other.syncMode, parentName, "syncMode", logger) &
                    isUnchanged(this.enabled, other.enabled, parentName, "enabled", logger) &
                    isUnchanged(this.mappers, other.mappers, parentName, "mappers", logger);
        }

        return result;
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

    public Long getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Long displayOrder) {
        this.displayOrder = displayOrder;
    }

    public List<String> getHostedDomains() {
        return hostedDomains;
    }

    public void setHostedDomains(List<String> hostedDomains) {
        this.hostedDomains = hostedDomains;
    }

    public Boolean getUseUserIpParam() {
        return useUserIpParam;
    }

    public void setUseUserIpParam(Boolean useUserIpParam) {
        this.useUserIpParam = useUserIpParam;
    }

    public Boolean getRequestRefreshToken() {
        return requestRefreshToken;
    }

    public void setRequestRefreshToken(Boolean requestRefreshToken) {
        this.requestRefreshToken = requestRefreshToken;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public Boolean getStoreTokens() {
        return storeTokens;
    }

    public void setStoreTokens(Boolean storeTokens) {
        this.storeTokens = storeTokens;
    }

    public Boolean getAcceptsPromptNoneForward() {
        return acceptsPromptNoneForward;
    }

    public void setAcceptsPromptNoneForward(Boolean acceptsPromptNoneForward) {
        this.acceptsPromptNoneForward = acceptsPromptNoneForward;
    }

    public Boolean getDisableUserInfo() {
        return disableUserInfo;
    }

    public void setDisableUserInfo(Boolean disableUserInfo) {
        this.disableUserInfo = disableUserInfo;
    }

    public Boolean getTrustEmail() {
        return trustEmail;
    }

    public void setTrustEmail(Boolean trustEmail) {
        this.trustEmail = trustEmail;
    }

    public Boolean getAccountLinkingOnly() {
        return accountLinkingOnly;
    }

    public void setAccountLinkingOnly(Boolean accountLinkingOnly) {
        this.accountLinkingOnly = accountLinkingOnly;
    }

    public Boolean getHideOnLoginPage() {
        return hideOnLoginPage;
    }

    public void setHideOnLoginPage(Boolean hideOnLoginPage) {
        this.hideOnLoginPage = hideOnLoginPage;
    }

    public EssentialClaim getEssentialClaim() {
        return essentialClaim;
    }

    public void setEssentialClaim(EssentialClaim essentialClaim) {
        this.essentialClaim = essentialClaim;
    }

    public String getFirstLoginFlowAlias() {
        return firstLoginFlowAlias;
    }

    public void setFirstLoginFlowAlias(String firstLoginFlowAlias) {
        this.firstLoginFlowAlias = firstLoginFlowAlias;
    }

    public String getPostLoginFlowAlias() {
        return postLoginFlowAlias;
    }

    public void setPostLoginFlowAlias(String postLoginFlowAlias) {
        this.postLoginFlowAlias = postLoginFlowAlias;
    }

    public String getSyncMode() {
        return syncMode;
    }

    public void setSyncMode(String syncMode) {
        this.syncMode = syncMode;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<IdentityProviderMapperDefinition> getMappers() {
        return mappers;
    }

    public void setMappers(List<IdentityProviderMapperDefinition> mappers) {
        this.mappers = mappers;
    }
}
