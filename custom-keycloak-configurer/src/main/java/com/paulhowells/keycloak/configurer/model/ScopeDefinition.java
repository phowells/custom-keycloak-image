package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(converter= ScopeDefinition.PostConstruct.class)
public class ScopeDefinition extends BaseDefinition {

    private String description;
    private String type;
    private String protocol;
    private Boolean includeInTokenScope;
    private Boolean displayOnConsentScreen;
    private String consentScreenText;

    // Internal Keycloak identifier
    @JsonIgnore
    private String id;
    @JsonIgnore
    private String name;
    @JsonIgnore
    private Boolean managed;

    public static class Mapper extends BaseDefinition {

        @JsonIgnore
        private String id;

        @JsonIgnore
        private String name;

        private String protocol;

        private String protocolMapper;

        private Boolean consentRequired;

        private Map<String, String> config = new HashMap<>();

        @Override
        public boolean isUnchanged(Object o, String parentName, Logger logger) {
            boolean result = super.isUnchanged(o, parentName, logger);

            if (result){

                Mapper other = (Mapper) o;

                result = isUnchanged(this.protocol, other.protocol, parentName, "protocol", logger) &
                        isUnchanged(this.protocolMapper, other.protocolMapper, parentName, "protocolMapper", logger) &
                        isUnchanged(this.consentRequired, other.consentRequired, parentName, "consentRequired", logger) &
                        isUnchanged(this.config, other.config, parentName, "config", logger);
            }

            return result;
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

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getProtocolMapper() {
            return protocolMapper;
        }

        public void setProtocolMapper(String protocolMapper) {
            this.protocolMapper = protocolMapper;
        }

        public Boolean getConsentRequired() {
            return consentRequired;
        }

        public void setConsentRequired(Boolean consentRequired) {
            this.consentRequired = consentRequired;
        }

        public Map<String, String> getConfig() {
            return config;
        }

        public void setConfig(Map<String, String> config) {
            this.config = config;
        }
    }

    private Map<String, Mapper> mappers = new HashMap<>();

    @Override
    public String toString() {
        return String.format("%s", name);
    }

    @Override
    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = super.isUnchanged(o, parentName, logger);

        if (result){

            ScopeDefinition other = (ScopeDefinition) o;

            result = isUnchanged(this.description, other.description, parentName, "description", logger) &
                    isUnchanged(this.type, other.type, parentName, "type", logger) &
                    isUnchanged(this.protocol, other.protocol, parentName, "protocol", logger) &
                    isUnchanged(this.includeInTokenScope, other.includeInTokenScope, parentName, "includeInTokenScope", logger) &
                    isUnchanged(this.displayOnConsentScreen, other.displayOnConsentScreen, parentName, "displayOnConsentScreen", logger) &
                    isUnchanged(this.consentScreenText, other.consentScreenText, parentName, "consentScreenText", logger);
        }

        return result;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Boolean getIncludeInTokenScope() {
        return includeInTokenScope;
    }

    public void setIncludeInTokenScope(Boolean includeInTokenScope) {
        this.includeInTokenScope = includeInTokenScope;
    }

    public Boolean getDisplayOnConsentScreen() {
        return displayOnConsentScreen;
    }

    public void setDisplayOnConsentScreen(Boolean displayOnConsentScreen) {
        this.displayOnConsentScreen = displayOnConsentScreen;
    }

    public String getConsentScreenText() {
        return consentScreenText;
    }

    public void setConsentScreenText(String consentScreenText) {
        this.consentScreenText = consentScreenText;
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

    public Map<String, Mapper> getMappers() {
        return mappers;
    }

    public void setMappers(Map<String, Mapper> mappers) {
        this.mappers = mappers;
    }

    static class PostConstruct extends StdConverter<ScopeDefinition, ScopeDefinition> {

        @Override
        public ScopeDefinition convert(ScopeDefinition o) {

            // Set the name on the roles
            for(String name:o.mappers.keySet()) {

                Mapper mapper = o.mappers.get(name);

                mapper.setName(name);
            }

            return o;
        }
    }
}
