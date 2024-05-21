package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TokenSettings extends BaseDefinition {
    private String defaultSignatureAlgorithm;
    private Boolean revokeRefreshToken;
    private Integer refreshTokenMaxReuse;
    private Integer accessTokenLifespanSeconds;
    private Integer clientLoginTimeoutSeconds;
    private Integer actionTokenGeneratedByUserLifespanSeconds;
    private Integer actionTokenGeneratedByAdminLifespanSeconds;

    @Override
    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = super.isUnchanged(o, parentName, logger);

        if (result){

            TokenSettings other = (TokenSettings) o;

            result = isUnchanged(this.defaultSignatureAlgorithm, other.defaultSignatureAlgorithm, parentName, "defaultSignatureAlgorithm", logger) &
                    isUnchanged(this.revokeRefreshToken, other.revokeRefreshToken, parentName, "revokeRefreshToken", logger) &
                    isUnchanged(this.refreshTokenMaxReuse, other.refreshTokenMaxReuse, parentName, "refreshTokenMaxReuse", logger) &
                    isUnchanged(this.accessTokenLifespanSeconds, other.accessTokenLifespanSeconds, parentName, "accessTokenLifespanSeconds", logger) &
                    isUnchanged(this.clientLoginTimeoutSeconds, other.clientLoginTimeoutSeconds, parentName, "clientLoginTimeoutSeconds", logger) &
                    isUnchanged(this.actionTokenGeneratedByUserLifespanSeconds, other.actionTokenGeneratedByUserLifespanSeconds, parentName, "actionTokenGeneratedByUserLifespanSeconds", logger) &
                    isUnchanged(this.actionTokenGeneratedByAdminLifespanSeconds, other.actionTokenGeneratedByAdminLifespanSeconds, parentName, "actionTokenGeneratedByAdminLifespanSeconds", logger);
        }

        return result;
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

    public Integer getAccessTokenLifespanSeconds() {
        return accessTokenLifespanSeconds;
    }

    public void setAccessTokenLifespanSeconds(Integer accessTokenLifespanSeconds) {
        this.accessTokenLifespanSeconds = accessTokenLifespanSeconds;
    }

    public Integer getClientLoginTimeoutSeconds() {
        return clientLoginTimeoutSeconds;
    }

    public void setClientLoginTimeoutSeconds(Integer clientLoginTimeoutSeconds) {
        this.clientLoginTimeoutSeconds = clientLoginTimeoutSeconds;
    }

    public Integer getActionTokenGeneratedByUserLifespanSeconds() {
        return actionTokenGeneratedByUserLifespanSeconds;
    }

    public void setActionTokenGeneratedByUserLifespanSeconds(Integer actionTokenGeneratedByUserLifespanSeconds) {
        this.actionTokenGeneratedByUserLifespanSeconds = actionTokenGeneratedByUserLifespanSeconds;
    }

    public Integer getActionTokenGeneratedByAdminLifespanSeconds() {
        return actionTokenGeneratedByAdminLifespanSeconds;
    }

    public void setActionTokenGeneratedByAdminLifespanSeconds(Integer actionTokenGeneratedByAdminLifespanSeconds) {
        this.actionTokenGeneratedByAdminLifespanSeconds = actionTokenGeneratedByAdminLifespanSeconds;
    }
}
