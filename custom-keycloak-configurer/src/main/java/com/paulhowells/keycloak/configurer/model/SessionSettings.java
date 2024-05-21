package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SessionSettings extends BaseDefinition {
    private Integer ssoSessionIdleTimeoutSeconds;
    private Integer ssoSessionMaxLifespanSeconds;
    private Integer ssoSessionIdleTimeoutRememberMeSeconds;
    private Integer ssoSessionMaxLifespanRememberMeSeconds;
    private Integer clientSessionIdleTimeoutSeconds;
    private Integer clientSessionMaxLifespanSeconds;
    private Integer offlineSessionIdleTimeoutSeconds;
    private Boolean offlineSessionMaxLifespanEnabled;
    private Integer offlineSessionMaxLifespanSeconds;
    private Integer accessCodeLifespanLoginSeconds;
    private Integer accessCodeLifespanUserActionSeconds;


    @Override
    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = super.isUnchanged(o, parentName, logger);

        if (result){

            SessionSettings other = (SessionSettings) o;

            result = isUnchanged(this.ssoSessionIdleTimeoutSeconds, other.ssoSessionIdleTimeoutSeconds, parentName, "ssoSessionIdleTimeoutSeconds", logger) &
                    isUnchanged(this.ssoSessionMaxLifespanSeconds, other.ssoSessionMaxLifespanSeconds, parentName, "ssoSessionMaxLifespanSeconds", logger) &
                    isUnchanged(this.ssoSessionIdleTimeoutRememberMeSeconds, other.ssoSessionIdleTimeoutRememberMeSeconds, parentName, "ssoSessionIdleTimeoutRememberMeSeconds", logger) &
                    isUnchanged(this.ssoSessionMaxLifespanRememberMeSeconds, other.ssoSessionMaxLifespanRememberMeSeconds, parentName, "ssoSessionMaxLifespanRememberMeSeconds", logger) &
                    isUnchanged(this.clientSessionIdleTimeoutSeconds, other.clientSessionIdleTimeoutSeconds, parentName, "clientSessionIdleTimeoutSeconds", logger) &
                    isUnchanged(this.clientSessionMaxLifespanSeconds, other.clientSessionMaxLifespanSeconds, parentName, "clientSessionMaxLifespanSeconds", logger) &
                    isUnchanged(this.offlineSessionIdleTimeoutSeconds, other.offlineSessionIdleTimeoutSeconds, parentName, "offlineSessionIdleTimeoutSeconds", logger) &
                    isUnchanged(this.offlineSessionMaxLifespanEnabled, other.offlineSessionMaxLifespanEnabled, parentName, "offlineSessionMaxLifespanEnabled", logger) &
                    isUnchanged(this.offlineSessionMaxLifespanSeconds, other.offlineSessionMaxLifespanSeconds, parentName, "offlineSessionMaxLifespanSeconds", logger) &
                    isUnchanged(this.accessCodeLifespanLoginSeconds, other.accessCodeLifespanLoginSeconds, parentName, "accessCodeLifespanLoginSeconds", logger) &
                    isUnchanged(this.accessCodeLifespanUserActionSeconds, other.accessCodeLifespanUserActionSeconds, parentName, "accessCodeLifespanUserActionSeconds", logger);
        }

        return result;
    }

    public Integer getSsoSessionIdleTimeoutSeconds() {
        return ssoSessionIdleTimeoutSeconds;
    }

    public void setSsoSessionIdleTimeoutSeconds(Integer ssoSessionIdleTimeoutSeconds) {
        this.ssoSessionIdleTimeoutSeconds = ssoSessionIdleTimeoutSeconds;
    }

    public Integer getSsoSessionMaxLifespanSeconds() {
        return ssoSessionMaxLifespanSeconds;
    }

    public void setSsoSessionMaxLifespanSeconds(Integer ssoSessionMaxLifespanSeconds) {
        this.ssoSessionMaxLifespanSeconds = ssoSessionMaxLifespanSeconds;
    }

    public Integer getSsoSessionIdleTimeoutRememberMeSeconds() {
        return ssoSessionIdleTimeoutRememberMeSeconds;
    }

    public void setSsoSessionIdleTimeoutRememberMeSeconds(Integer ssoSessionIdleTimeoutRememberMeSeconds) {
        this.ssoSessionIdleTimeoutRememberMeSeconds = ssoSessionIdleTimeoutRememberMeSeconds;
    }

    public Integer getSsoSessionMaxLifespanRememberMeSeconds() {
        return ssoSessionMaxLifespanRememberMeSeconds;
    }

    public void setSsoSessionMaxLifespanRememberMeSeconds(Integer ssoSessionMaxLifespanRememberMeSeconds) {
        this.ssoSessionMaxLifespanRememberMeSeconds = ssoSessionMaxLifespanRememberMeSeconds;
    }

    public Integer getClientSessionIdleTimeoutSeconds() {
        return clientSessionIdleTimeoutSeconds;
    }

    public void setClientSessionIdleTimeoutSeconds(Integer clientSessionIdleTimeoutSeconds) {
        this.clientSessionIdleTimeoutSeconds = clientSessionIdleTimeoutSeconds;
    }

    public Integer getClientSessionMaxLifespanSeconds() {
        return clientSessionMaxLifespanSeconds;
    }

    public void setClientSessionMaxLifespanSeconds(Integer clientSessionMaxLifespanSeconds) {
        this.clientSessionMaxLifespanSeconds = clientSessionMaxLifespanSeconds;
    }

    public Integer getOfflineSessionIdleTimeoutSeconds() {
        return offlineSessionIdleTimeoutSeconds;
    }

    public void setOfflineSessionIdleTimeoutSeconds(Integer offlineSessionIdleTimeoutSeconds) {
        this.offlineSessionIdleTimeoutSeconds = offlineSessionIdleTimeoutSeconds;
    }

    public Boolean getOfflineSessionMaxLifespanEnabled() {
        return offlineSessionMaxLifespanEnabled;
    }

    public void setOfflineSessionMaxLifespanEnabled(Boolean offlineSessionMaxLifespanEnabled) {
        this.offlineSessionMaxLifespanEnabled = offlineSessionMaxLifespanEnabled;
    }

    public Integer getOfflineSessionMaxLifespanSeconds() {
        return offlineSessionMaxLifespanSeconds;
    }

    public void setOfflineSessionMaxLifespanSeconds(Integer offlineSessionMaxLifespanSeconds) {
        this.offlineSessionMaxLifespanSeconds = offlineSessionMaxLifespanSeconds;
    }

    public Integer getAccessCodeLifespanLoginSeconds() {
        return accessCodeLifespanLoginSeconds;
    }

    public void setAccessCodeLifespanLoginSeconds(Integer accessCodeLifespanLoginSeconds) {
        this.accessCodeLifespanLoginSeconds = accessCodeLifespanLoginSeconds;
    }

    public Integer getAccessCodeLifespanUserActionSeconds() {
        return accessCodeLifespanUserActionSeconds;
    }

    public void setAccessCodeLifespanUserActionSeconds(Integer accessCodeLifespanUserActionSeconds) {
        this.accessCodeLifespanUserActionSeconds = accessCodeLifespanUserActionSeconds;
    }
}
