package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class KeycloakDefinition {

    private Boolean deleteManagedResourcesWhenRemoved = Boolean.FALSE;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<RealmDefinition> realms = new ArrayList<>();
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<ClientDefinition> clients = new ArrayList<>();
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<AuthenticationFlowDefinition> authenticationFlows = new ArrayList<>();

    public Boolean getDeleteManagedResourcesWhenRemoved() {
        return deleteManagedResourcesWhenRemoved;
    }

    public void setDeleteManagedResourcesWhenRemoved(Boolean deleteManagedResourcesWhenRemoved) {
        this.deleteManagedResourcesWhenRemoved = deleteManagedResourcesWhenRemoved;
    }

    public List<RealmDefinition> getRealms() {
        return realms;
    }

    public void setRealms(List<RealmDefinition> realms) {
        this.realms = realms;
    }

    public List<ClientDefinition> getClients() {
        return clients;
    }

    public void setClients(List<ClientDefinition> clients) {
        this.clients = clients;
    }

    public List<AuthenticationFlowDefinition> getAuthenticationFlows() {
        return authenticationFlows;
    }

    public void setAuthenticationFlows(List<AuthenticationFlowDefinition> authenticationFlows) {
        this.authenticationFlows = authenticationFlows;
    }
}
