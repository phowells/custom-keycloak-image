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
public class Scope extends BaseModel {

    private String id;

    private String name;

    private String description;

    private String protocol;

    private String type;

    private Map<String, String> attributes = new HashMap<>();

    private List<ProtocolMapper> protocolMapper = new ArrayList<>();

    @JsonIgnore
    private String realm;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String,String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String,String> attributes) {
        this.attributes = attributes;
    }

    public List<ProtocolMapper> getProtocolMapper() {
        return protocolMapper;
    }

    public void setProtocolMapper(List<ProtocolMapper> protocolMapper) {
        this.protocolMapper = protocolMapper;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }
}
