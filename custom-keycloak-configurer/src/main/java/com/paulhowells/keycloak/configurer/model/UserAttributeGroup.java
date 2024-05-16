package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserAttributeGroup extends BaseDefinition {
    @JsonIgnore
    private String name;
    private String displayHeader;
    private String displayDescription;

    public String toString() {

        return name;
    }

    @Override
    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = super.isUnchanged(o, parentName, logger);

        if (result){

            UserAttributeGroup other = (UserAttributeGroup) o;

            result = isUnchanged(this.displayHeader, other.displayHeader, parentName, "displayHeader", logger) &
                    isUnchanged(this.displayDescription, other.displayDescription, parentName, "displayDescription", logger);
        }

        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayHeader() {
        return displayHeader;
    }

    public void setDisplayHeader(String displayHeader) {
        this.displayHeader = displayHeader;
    }

    public String getDisplayDescription() {
        return displayDescription;
    }

    public void setDisplayDescription(String displayDescription) {
        this.displayDescription = displayDescription;
    }
}
