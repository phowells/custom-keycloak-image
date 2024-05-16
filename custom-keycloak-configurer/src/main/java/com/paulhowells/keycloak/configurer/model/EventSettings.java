package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class EventSettings extends BaseDefinition {

    private List<String> eventsListeners;
    private Boolean userEventsEnabled;
    private Integer userEventsExpirationSeconds;
    private List<String> enabledEventTypes;
    private Boolean adminEventsEnabled;
    private Boolean adminEventsDetailsEnabled;
    private Integer adminEventsExpirationSeconds;

    @Override
    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = super.isUnchanged(o, parentName, logger);

        if (result){

            EventSettings other = (EventSettings) o;

            result = isUnchanged(this.eventsListeners, other.eventsListeners, parentName, "eventsListeners", logger) &
                    isUnchanged(this.userEventsEnabled, other.userEventsEnabled, parentName, "userEventsEnabled", logger) &
                    isUnchanged(this.userEventsExpirationSeconds, other.userEventsExpirationSeconds, parentName, "userEventsExpirationSeconds", logger) &
                    isUnchanged(this.enabledEventTypes, other.enabledEventTypes, parentName, "enabledEventTypes", logger) &
                    isUnchanged(this.adminEventsEnabled, other.adminEventsEnabled, parentName, "adminEventsEnabled", logger) &
                    isUnchanged(this.adminEventsDetailsEnabled, other.adminEventsDetailsEnabled, parentName, "adminEventsDetailsEnabled", logger) &
                    isUnchanged(this.adminEventsExpirationSeconds, other.adminEventsExpirationSeconds, parentName, "adminEventsExpirationSeconds", logger);
        }

        return result;
    }

    public List<String> getEventsListeners() {
        return eventsListeners;
    }

    public void setEventsListeners(List<String> eventsListeners) {
        this.eventsListeners = eventsListeners;
    }

    public Boolean getUserEventsEnabled() {
        return userEventsEnabled;
    }

    public void setUserEventsEnabled(Boolean userEventsEnabled) {
        this.userEventsEnabled = userEventsEnabled;
    }

    public Integer getUserEventsExpirationSeconds() {
        return userEventsExpirationSeconds;
    }

    public void setUserEventsExpirationSeconds(Integer userEventsExpirationSeconds) {
        this.userEventsExpirationSeconds = userEventsExpirationSeconds;
    }

    public List<String> getEnabledEventTypes() {
        return enabledEventTypes;
    }

    public void setEnabledEventTypes(List<String> enabledEventTypes) {
        this.enabledEventTypes = enabledEventTypes;
    }

    public Boolean getAdminEventsEnabled() {
        return adminEventsEnabled;
    }

    public void setAdminEventsEnabled(Boolean adminEventsEnabled) {
        this.adminEventsEnabled = adminEventsEnabled;
    }

    public Boolean getAdminEventsDetailsEnabled() {
        return adminEventsDetailsEnabled;
    }

    public void setAdminEventsDetailsEnabled(Boolean adminEventsDetailsEnabled) {
        this.adminEventsDetailsEnabled = adminEventsDetailsEnabled;
    }

    public Integer getAdminEventsExpirationSeconds() {
        return adminEventsExpirationSeconds;
    }

    public void setAdminEventsExpirationSeconds(Integer adminEventsExpirationSeconds) {
        this.adminEventsExpirationSeconds = adminEventsExpirationSeconds;
    }
}
