package com.paulhowells.keycloak.configurer.rest.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Realm extends BaseModel {
    public static final String BROWSER_FLOW_KEY = "browser";
    public static final String DIRECT_GRANT_FLOW_KEY = "direct_grant";

    private String realm;
    private String browserFlow;
    private String registrationFlow;
    private String directGrantFlow;
    private String resetCredentialsFlow;
    private String clientAuthenticationFlow;
    private String firstBrokerLoginFlowAlias;
    private Boolean userManagedAccessAllowed;
    private Boolean registrationAllowed;
    private Boolean registrationEmailAsUsername;
    private Boolean rememberMe;
    private Boolean verifyEmail;
    private Boolean loginWithEmailAllowed;
    private Boolean duplicateEmailsAllowed;
    private Boolean resetPasswordAllowed;
    private Boolean editUsernameAllowed;
    private String passwordPolicy;
    private Boolean enabled;
    private Map<String, String> attributes = new HashMap<>();
    private List<IdentityProvider> identityProviders = new ArrayList<>();
    private SmtpServer smtpServer;
    private Integer ssoSessionIdleTimeout;
    private Integer ssoSessionMaxLifespan;
    private Integer ssoSessionIdleTimeoutRememberMe;
    private Integer ssoSessionMaxLifespanRememberMe;
    private Integer clientSessionIdleTimeout;
    private Integer clientSessionMaxLifespan;
    private Integer offlineSessionIdleTimeout;
    private Boolean offlineSessionMaxLifespanEnabled;
    private Integer offlineSessionMaxLifespan;
    private Integer accessCodeLifespanLogin;
    private Integer accessCodeLifespanUserAction;
    private String defaultSignatureAlgorithm;
    private Boolean revokeRefreshToken;
    private Integer refreshTokenMaxReuse;
    private Integer accessTokenLifespan;
    private Integer accessCodeLifespan;
    private Integer actionTokenGeneratedByUserLifespan;
    private Integer actionTokenGeneratedByAdminLifespan;

    private Role defaultRole;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getBrowserFlow() {
        return browserFlow;
    }

    public void setBrowserFlow(String browserFlow) {
        this.browserFlow = browserFlow;
    }

    public String getRegistrationFlow() {
        return registrationFlow;
    }

    public void setRegistrationFlow(String registrationFlow) {
        this.registrationFlow = registrationFlow;
    }

    public String getDirectGrantFlow() {
        return directGrantFlow;
    }

    public void setDirectGrantFlow(String directGrantFlow) {
        this.directGrantFlow = directGrantFlow;
    }

    public String getResetCredentialsFlow() {
        return resetCredentialsFlow;
    }

    public void setResetCredentialsFlow(String resetCredentialsFlow) {
        this.resetCredentialsFlow = resetCredentialsFlow;
    }

    public String getClientAuthenticationFlow() {
        return clientAuthenticationFlow;
    }

    public void setClientAuthenticationFlow(String clientAuthenticationFlow) {
        this.clientAuthenticationFlow = clientAuthenticationFlow;
    }

    public String getFirstBrokerLoginFlowAlias() {
        return firstBrokerLoginFlowAlias;
    }

    public void setFirstBrokerLoginFlowAlias(String firstBrokerLoginFlowAlias) {
        this.firstBrokerLoginFlowAlias = firstBrokerLoginFlowAlias;
    }

    public Boolean getUserManagedAccessAllowed() {
        return userManagedAccessAllowed;
    }

    public void setUserManagedAccessAllowed(Boolean userManagedAccessAllowed) {
        this.userManagedAccessAllowed = userManagedAccessAllowed;
    }

    public Boolean getRegistrationAllowed() {
        return registrationAllowed;
    }

    public void setRegistrationAllowed(Boolean registrationAllowed) {
        this.registrationAllowed = registrationAllowed;
    }

    public Boolean getRegistrationEmailAsUsername() {
        return registrationEmailAsUsername;
    }

    public void setRegistrationEmailAsUsername(Boolean registrationEmailAsUsername) {
        this.registrationEmailAsUsername = registrationEmailAsUsername;
    }

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public Boolean getVerifyEmail() {
        return verifyEmail;
    }

    public void setVerifyEmail(Boolean verifyEmail) {
        this.verifyEmail = verifyEmail;
    }

    public Boolean getLoginWithEmailAllowed() {
        return loginWithEmailAllowed;
    }

    public void setLoginWithEmailAllowed(Boolean loginWithEmailAllowed) {
        this.loginWithEmailAllowed = loginWithEmailAllowed;
    }

    public Boolean getDuplicateEmailsAllowed() {
        return duplicateEmailsAllowed;
    }

    public void setDuplicateEmailsAllowed(Boolean duplicateEmailsAllowed) {
        this.duplicateEmailsAllowed = duplicateEmailsAllowed;
    }

    public Boolean getResetPasswordAllowed() {
        return resetPasswordAllowed;
    }

    public void setResetPasswordAllowed(Boolean resetPasswordAllowed) {
        this.resetPasswordAllowed = resetPasswordAllowed;
    }

    public Boolean getEditUsernameAllowed() {
        return editUsernameAllowed;
    }

    public void setEditUsernameAllowed(Boolean editUsernameAllowed) {
        this.editUsernameAllowed = editUsernameAllowed;
    }

    public String getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(String passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public List<IdentityProvider> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(List<IdentityProvider> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public SmtpServer getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(SmtpServer smtpServer) {
        this.smtpServer = smtpServer;
    }

    public Integer getSsoSessionIdleTimeout() {
        return ssoSessionIdleTimeout;
    }

    public void setSsoSessionIdleTimeout(Integer ssoSessionIdleTimeout) {
        this.ssoSessionIdleTimeout = ssoSessionIdleTimeout;
    }

    public Integer getSsoSessionMaxLifespan() {
        return ssoSessionMaxLifespan;
    }

    public void setSsoSessionMaxLifespan(Integer ssoSessionMaxLifespan) {
        this.ssoSessionMaxLifespan = ssoSessionMaxLifespan;
    }

    public Integer getSsoSessionIdleTimeoutRememberMe() {
        return ssoSessionIdleTimeoutRememberMe;
    }

    public void setSsoSessionIdleTimeoutRememberMe(Integer ssoSessionIdleTimeoutRememberMe) {
        this.ssoSessionIdleTimeoutRememberMe = ssoSessionIdleTimeoutRememberMe;
    }

    public Integer getSsoSessionMaxLifespanRememberMe() {
        return ssoSessionMaxLifespanRememberMe;
    }

    public void setSsoSessionMaxLifespanRememberMe(Integer ssoSessionMaxLifespanRememberMe) {
        this.ssoSessionMaxLifespanRememberMe = ssoSessionMaxLifespanRememberMe;
    }

    public Integer getClientSessionIdleTimeout() {
        return clientSessionIdleTimeout;
    }

    public void setClientSessionIdleTimeout(Integer clientSessionIdleTimeout) {
        this.clientSessionIdleTimeout = clientSessionIdleTimeout;
    }

    public Integer getClientSessionMaxLifespan() {
        return clientSessionMaxLifespan;
    }

    public void setClientSessionMaxLifespan(Integer clientSessionMaxLifespan) {
        this.clientSessionMaxLifespan = clientSessionMaxLifespan;
    }

    public Integer getOfflineSessionIdleTimeout() {
        return offlineSessionIdleTimeout;
    }

    public void setOfflineSessionIdleTimeout(Integer offlineSessionIdleTimeout) {
        this.offlineSessionIdleTimeout = offlineSessionIdleTimeout;
    }

    public Boolean getOfflineSessionMaxLifespanEnabled() {
        return offlineSessionMaxLifespanEnabled;
    }

    public void setOfflineSessionMaxLifespanEnabled(Boolean offlineSessionMaxLifespanEnabled) {
        this.offlineSessionMaxLifespanEnabled = offlineSessionMaxLifespanEnabled;
    }

    public Integer getOfflineSessionMaxLifespan() {
        return offlineSessionMaxLifespan;
    }

    public void setOfflineSessionMaxLifespan(Integer offlineSessionMaxLifespan) {
        this.offlineSessionMaxLifespan = offlineSessionMaxLifespan;
    }

    public Integer getAccessCodeLifespanLogin() {
        return accessCodeLifespanLogin;
    }

    public void setAccessCodeLifespanLogin(Integer accessCodeLifespanLogin) {
        this.accessCodeLifespanLogin = accessCodeLifespanLogin;
    }

    public Integer getAccessCodeLifespanUserAction() {
        return accessCodeLifespanUserAction;
    }

    public void setAccessCodeLifespanUserAction(Integer accessCodeLifespanUserAction) {
        this.accessCodeLifespanUserAction = accessCodeLifespanUserAction;
    }

    public String getDefaultSignatureAlgorithm() {
        return defaultSignatureAlgorithm;
    }

    public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {
        this.defaultSignatureAlgorithm = defaultSignatureAlgorithm;
    }

    public Boolean getRevokeRefreshToken() {
        return revokeRefreshToken;
    }

    public void setRevokeRefreshToken(Boolean revokeRefreshToken) {
        this.revokeRefreshToken = revokeRefreshToken;
    }

    public Integer getRefreshTokenMaxReuse() {
        return refreshTokenMaxReuse;
    }

    public void setRefreshTokenMaxReuse(Integer refreshTokenMaxReuse) {
        this.refreshTokenMaxReuse = refreshTokenMaxReuse;
    }

    public Integer getAccessTokenLifespan() {
        return accessTokenLifespan;
    }

    public void setAccessTokenLifespan(Integer accessTokenLifespan) {
        this.accessTokenLifespan = accessTokenLifespan;
    }

    public Integer getAccessCodeLifespan() {
        return accessCodeLifespan;
    }

    public void setAccessCodeLifespan(Integer accessCodeLifespan) {
        this.accessCodeLifespan = accessCodeLifespan;
    }

    public Integer getActionTokenGeneratedByUserLifespan() {
        return actionTokenGeneratedByUserLifespan;
    }

    public void setActionTokenGeneratedByUserLifespan(Integer actionTokenGeneratedByUserLifespan) {
        this.actionTokenGeneratedByUserLifespan = actionTokenGeneratedByUserLifespan;
    }

    public Integer getActionTokenGeneratedByAdminLifespan() {
        return actionTokenGeneratedByAdminLifespan;
    }

    public void setActionTokenGeneratedByAdminLifespan(Integer actionTokenGeneratedByAdminLifespan) {
        this.actionTokenGeneratedByAdminLifespan = actionTokenGeneratedByAdminLifespan;
    }

    public Role getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(Role defaultRole) {
        this.defaultRole = defaultRole;
    }
}
