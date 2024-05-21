package com.paulhowells.keycloak.configurer.rest.client.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public class BaseModel {

    @JsonIgnore
    private Map<String, Object> _raw;

    public Map<String, Object> get_raw() {
        return _raw;
    }

    public void set_raw(Map<String, Object> _raw) {
        this._raw = _raw;
    }
}
