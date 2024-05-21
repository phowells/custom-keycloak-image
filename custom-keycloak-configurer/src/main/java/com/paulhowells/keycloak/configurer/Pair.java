package com.paulhowells.keycloak.configurer;


import com.paulhowells.keycloak.configurer.model.AuthenticationFlowDefinition;
import com.paulhowells.keycloak.configurer.model.ClientDefinition;
import com.paulhowells.keycloak.configurer.model.KeycloakDefinition;
import com.paulhowells.keycloak.configurer.model.RealmDefinition;

import java.util.Collection;

public record Pair<X, Y>(X first, Y second) {

    static KeycloakDefinition createKeycloakDefinition(
            boolean deleteManagedResourcesWhenRemoved,
            Collection<Pair<String, RealmDefinition>> realms,
            Collection<Pair<String, AuthenticationFlowDefinition>> authenticationFlows,
            Collection<Pair<String, ClientDefinition>> clients
    ) {

        KeycloakDefinition result = new KeycloakDefinition();
        result.setDeleteManagedResourcesWhenRemoved(deleteManagedResourcesWhenRemoved);
        result.setRealms(realms.stream().map(it -> it.second).toList());
        result.setAuthenticationFlows(authenticationFlows.stream().map(it -> it.second).toList());
        result.setClients(clients.stream().map(it -> it.second).toList());
        return result;
    }
}