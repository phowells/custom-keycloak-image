package com.paulhowells.keycloak.configurer.rest.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Attribute extends BaseModel {

    private String name;
    private String displayName;
    private String group;
    private Map<String, Object> validations = new HashMap<>();

    public static class Permissions {
        private List<String> view = new ArrayList<>();
        private List<String> edit = new ArrayList<>();

        public List<String> getView() {
            return view;
        }

        public void setView(List<String> view) {
            this.view = view;
        }

        public List<String> getEdit() {
            return edit;
        }

        public void setEdit(List<String> edit) {
            this.edit = edit;
        }
    }
    private Permissions permissions = new Permissions();
    private Boolean multivalued;

    public static class Required {
        private List<String> roles = new ArrayList<>();
        private List<String> scopes = new ArrayList<>();

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }
    }
    private Required required = new Required();
    private Map<String, String> annotations = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Map<String, Object> getValidations() {
        return validations;
    }

    public void setValidations(Map<String, Object> validations) {
        this.validations = validations;
    }

    public Permissions getPermissions() {
        return permissions;
    }

    public void setPermissions(Permissions permissions) {
        this.permissions = permissions;
    }

    public Boolean getMultivalued() {
        return multivalued;
    }

    public void setMultivalued(Boolean multivalued) {
        this.multivalued = multivalued;
    }

    public Required getRequired() {
        return required;
    }

    public void setRequired(Required required) {
        this.required = required;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Map<String, String> annotations) {
        this.annotations = annotations;
    }
}
