package com.paulhowells.keycloak.configurer.rest.client;

import com.paulhowells.keycloak.configurer.rest.client.model.BaseModel;

import java.util.List;
import java.util.Map;

public class KeycloakListResponse<T extends BaseModel> extends KeycloakAbstractResponse {

    public List<T> body;
}
