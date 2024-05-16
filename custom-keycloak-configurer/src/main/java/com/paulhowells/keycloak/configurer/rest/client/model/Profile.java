package com.paulhowells.keycloak.configurer.rest.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Profile extends BaseModel {

    private List<Attribute> attributes;
    private List<AttributeGroup> groups;
    private String unmanagedAttributePolicy;

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public List<AttributeGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<AttributeGroup> groups) {
        this.groups = groups;
    }

    public String getUnmanagedAttributePolicy() {
        return unmanagedAttributePolicy;
    }

    public void setUnmanagedAttributePolicy(String unmanagedAttributePolicy) {
        this.unmanagedAttributePolicy = unmanagedAttributePolicy;
    }
}
