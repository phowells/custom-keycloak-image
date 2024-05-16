package com.paulhowells.keycloak.configurer.rest.client.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Client extends BaseModel {

    public static final String DEVICE_AUTHORIZATION_GRANT_ATTRIBUTE = "oauth2.device.authorization.grant.enabled";
    public static final String OIDC_CIBA_GRANT_ATTRIBUTE = "oidc.ciba.grant.enabled";
    public static final String POST_LOGOUT_REDIRECT_URIS_ATTRIBUTE = "post.logout.redirect.uris";
    public static final String ACCESS_TOKEN_LIFESPAN_ATTRIBUTE = "access.token.lifespan";
    public static final String CLIENT_SESSION_IDLE_TIMEOUT_SECONDS_ATTRIBUTE = "client.session.idle.timeout";
    public static final String CLIENT_SESSION_MAX_LIFESPAN_SECONDS_ATTRIBUTE = "client.session.max.lifespan";
    public static final String CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT_SECONDS_ATTRIBUTE = "client.offline.session.idle.timeout";
    public static final String CLIENT_OFFLINE_SESSION_MAX_LIFESPAN_SECONDS_ATTRIBUTE = "client.offline.session.max.lifespan";

    private String id;

    private String clientId;

    private String secret;

    private Boolean publicClient;

    private Boolean serviceAccountsEnabled;

    private Boolean standardFlowEnabled;

    private Boolean directAccessGrantsEnabled;

    private Boolean implicitFlowEnabled;

    private Boolean enabled;

    private List<String> redirectUris = new ArrayList<>();

    private List<String> webOrigins = new ArrayList<>();

    private Map<String, String> attributes = new HashMap<>();

    private Map<String, String> authenticationFlowBindingOverrides = new HashMap<>();

    private List<String> defaultClientScopes = new ArrayList<>();

    private List<String> optionalClientScopes = new ArrayList<>();

    @JsonIgnore
    private String realm;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Boolean getPublicClient() {
        return publicClient;
    }

    public void setPublicClient(Boolean publicClient) {
        this.publicClient = publicClient;
    }

    public Boolean getServiceAccountsEnabled() {
        return serviceAccountsEnabled;
    }

    public void setServiceAccountsEnabled(Boolean serviceAccountsEnabled) {
        this.serviceAccountsEnabled = serviceAccountsEnabled;
    }

    public Boolean getStandardFlowEnabled() {
        return standardFlowEnabled;
    }

    public void setStandardFlowEnabled(Boolean standardFlowEnabled) {
        this.standardFlowEnabled = standardFlowEnabled;
    }

    public Boolean getDirectAccessGrantsEnabled() {
        return directAccessGrantsEnabled;
    }

    public void setDirectAccessGrantsEnabled(Boolean directAccessGrantsEnabled) {
        this.directAccessGrantsEnabled = directAccessGrantsEnabled;
    }

    public Boolean getImplicitFlowEnabled() {
        return implicitFlowEnabled;
    }

    public void setImplicitFlowEnabled(Boolean implicitFlowEnabled) {
        this.implicitFlowEnabled = implicitFlowEnabled;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public List<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(List<String> webOrigins) {
        this.webOrigins = webOrigins;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        return authenticationFlowBindingOverrides;
    }

    public void setAuthenticationFlowBindingOverrides(Map<String, String> authenticationFlowBindingOverrides) {
        this.authenticationFlowBindingOverrides = authenticationFlowBindingOverrides;
    }

    public List<String> getDefaultClientScopes() {
        return defaultClientScopes;
    }

    public void setDefaultClientScopes(List<String> defaultClientScopes) {
        this.defaultClientScopes = defaultClientScopes;
    }

    public List<String> getOptionalClientScopes() {
        return optionalClientScopes;
    }

    public void setOptionalClientScopes(List<String> optionalClientScopes) {
        this.optionalClientScopes = optionalClientScopes;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }
}
