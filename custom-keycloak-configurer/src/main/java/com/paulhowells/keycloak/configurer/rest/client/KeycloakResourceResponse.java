package com.paulhowells.keycloak.configurer.rest.client;

import com.paulhowells.keycloak.configurer.rest.client.model.BaseModel;

import java.util.Map;

public class KeycloakResourceResponse<T extends BaseModel> extends KeycloakAbstractResponse {

    public T body;
}
