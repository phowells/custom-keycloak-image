package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GeneralSettings extends BaseDefinition {
    private Boolean userManagedAccessAllowed;

    @Override
    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = super.isUnchanged(o, parentName, logger);

        if (result){

            GeneralSettings other = (GeneralSettings) o;

            result = isUnchanged(this.userManagedAccessAllowed, other.userManagedAccessAllowed, parentName, "userManagedAccessAllowed", logger);
        }

        return result;
    }

    public Boolean getUserManagedAccessAllowed() {
        return userManagedAccessAllowed;
    }

    public void setUserManagedAccessAllowed(Boolean userManagedAccessAllowed) {
        this.userManagedAccessAllowed = userManagedAccessAllowed;
    }
}
