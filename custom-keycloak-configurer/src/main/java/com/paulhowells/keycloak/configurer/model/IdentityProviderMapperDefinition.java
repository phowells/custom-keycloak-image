package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class IdentityProviderMapperDefinition extends BaseDefinition {
    private String name;
    private String identityProviderMapper;
    private Map<String, Object> config = new HashMap<>();

    @JsonIgnore
    private String id;

    @Override
    public String toString() {
        return String.format("%s(%s)", identityProviderMapper, name);
    }

    @Override
    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = super.isUnchanged(o, parentName, logger);

        if (result){

            IdentityProviderMapperDefinition other = (IdentityProviderMapperDefinition) o;

            result = isUnchanged(this.name, other.name, parentName, "name", logger) &
                    isUnchanged(this.identityProviderMapper, other.identityProviderMapper, parentName, "identityProviderMapper", logger) &
                    isUnchanged(this.config, other.config, parentName, "config", logger);
        }

        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentityProviderMapper() {
        return identityProviderMapper;
    }

    public void setIdentityProviderMapper(String identityProviderMapper) {
        this.identityProviderMapper = identityProviderMapper;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
