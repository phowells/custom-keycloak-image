package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RoleDefinition extends BaseDefinition {
    private String description;

    // Internal Keycloak identifier
    @JsonIgnore
    private String id;
    @JsonIgnore
    private String name;
    @JsonIgnore
    private Boolean managed;

    @Override
    public String toString() {
        return String.format("%s", name);
    }

    @Override
    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = super.isUnchanged(o, parentName, logger);

        if (result){

            RoleDefinition other = (RoleDefinition) o;

            result = isUnchanged(this.description, other.description, parentName, "description", logger);
        }

        return result;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getManaged() {
        return managed;
    }

    public void setManaged(Boolean managed) {
        this.managed = managed;
    }
}
