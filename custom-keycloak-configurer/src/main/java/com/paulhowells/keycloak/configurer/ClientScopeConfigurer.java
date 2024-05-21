package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.ClientDefinition;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.Client;
import com.paulhowells.keycloak.configurer.rest.client.model.DefaultScope;
import com.paulhowells.keycloak.configurer.rest.client.model.Scope;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClientScopeConfigurer {

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    ClientScopeConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
    }

    boolean applyScopes(
            Client client,
            ClientDefinition clientDefinition
    ) {

        List<ClientDefinition.Scope> insertDefinitions = new ArrayList<>();
        Map<String, ClientDefinition.Scope> updateDefinitionMap = new HashMap<>();
        Set<Scope> referencedResources = new HashSet<>();
        Map<String, Scope> existingResourceMap = getResourceMap(client);

        // Identify if the resource is new or if a resource needs to be updated
        // Keep track of which resource are in use, so we know which ones need to be deleted later
        identifyUpdates(
                existingResourceMap,
                clientDefinition,
                insertDefinitions,
                updateDefinitionMap,
                referencedResources
        );

        List<String> referencedResourceKeys = getReferencedResourceKeys(referencedResources);
        Map<String, Scope> removedResourceMap = getRemovedResourceMap(existingResourceMap, referencedResourceKeys);

        return processUpdates(
                client,
                removedResourceMap,
                insertDefinitions,
                updateDefinitionMap
        );
    }

    private Map<String, Scope> getResourceMap(
            Client client
    ) {
        Map<String, Scope> results = new HashMap<>();

        String realmName = client.getRealm();

        List<Scope> scopes = getScopes(realmName, client.getId());

        for (Scope scope:scopes) {

            String scopeKey = getKey(realmName, scope.getName());

            results.put(scopeKey, scope);
        }

        return results;
    }

    private void identifyUpdates(
            Map<String, Scope> existingResourceMap,
            ClientDefinition clientDefinition,
            List<ClientDefinition.Scope> insertDefinitions,
            Map<String, ClientDefinition.Scope> updateDefinitionsPkMap,
            Set<Scope> referencedResources
    ) {

        // Identify if the definition is new or if an existing definition needs to be updated
        // Keep track of which definitions are in use, so we know which ones need to be deleted later
        for (String scopeName: clientDefinition.getScopes().keySet()) {

            ClientDefinition.Scope definition = clientDefinition.getScopes().get(scopeName);

            String key = getKey(clientDefinition.getRealmName(), definition.getName());

            Scope existingResource = existingResourceMap.get(key);
            if (existingResource == null) {

                insertDefinitions.add(definition);
            } else {

                definition.setId(existingResource.getId());
                updateDefinitionsPkMap.put(key, definition);
                referencedResources.add(existingResource);
            }
        }
    }

    private boolean processUpdates(
            Client client,
            Map<String, Scope> removedResourceMap,
            List<ClientDefinition.Scope> insertDefinitions,
            Map<String, ClientDefinition.Scope> updateDefinitionMap
    ) {
        boolean result = false;

        // Delete the obsolete  Scopes
        for (String scopeKey:removedResourceMap.keySet()) {

            Scope scope = removedResourceMap.get(scopeKey);

            result = removeResource(
                    client,
                    scope
            ) || result;
        }

        Map<String, Scope> realmScopesMap = keycloakRestApi.getClientScopes(client.getRealm()).stream()
                .collect(Collectors.toMap(Scope::getName, Function.identity()));

        // Insert the new  Scopes
        for (ClientDefinition.Scope scopeDefinition:insertDefinitions) {

            createResource(
                    client,
                    scopeDefinition,
                    realmScopesMap
            );
            result = true;
        }

        Map<String, Scope> clientScopesMap = getScopes(client.getRealm(), client.getId()).stream()
                .collect(Collectors.toMap(Scope::getName, Function.identity()));

        // Update the existing  Scopes
        for (ClientDefinition.Scope scopeDefinition:updateDefinitionMap.values()) {

            result = updateResource(
                    client,
                    scopeDefinition,
                    clientScopesMap
            ) || result;
        }

        return result;
    }

    private static boolean isManaged(Scope resource) {

        return !"none".equals(resource.getType());
    }

    private boolean removeResource(
            Client client,
            Scope scope
    ) {
        logger.debug("<removeResource");
        boolean result = false;

        if (isManaged(scope)) {

            logger.warn("Deleting  {} {} scope {}", client.getRealm(), client.getClientId(), scope.getName());

            switch (scope.getType()) {
                case "default": {
                    keycloakRestApi.deleteDefaultScope(client.getRealm(), client.getId(), scope.getId());
                    break;
                }
                case "optional": {
                    keycloakRestApi.deleteOptionalScope(client.getRealm(), client.getId(), scope.getId());
                    break;
                }

            }
            result = true;
        }

        logger.debug(">removeResource");
        return result;
    }

    private void createResource(
            Client client,
            ClientDefinition.Scope scopeDefinition,
            Map<String, Scope> realmScopesMap
    ) {
        logger.debug("<createResource");

        logger.info("Creating {} {} scope {}", client.getRealm(), client.getClientId(), scopeDefinition.getName());

        Scope scope = realmScopesMap.get(scopeDefinition.getName());

        switch(scopeDefinition.getType()) {
            case "default": {
                keycloakRestApi.addDefaultScope(client.getRealm(), client.getId(), scope.getId());
                break;
            }
            case "optional": {
                keycloakRestApi.addOptionalScope(client.getRealm(), client.getId(), scope.getId());
                break;
            }
            default: throw new IllegalStateException(String.format("Note expecting scope type: %s", scopeDefinition.getType()));
        }

        logger.debug(">createResource");
    }

    private boolean updateResource(
            Client client,
            ClientDefinition.Scope scopeDefinition,
            Map<String, Scope> clientScopesMap
    ) {
        logger.debug("<updateResource");
        boolean result = false;

        Scope scope = clientScopesMap.get(scopeDefinition.getName());

        ClientDefinition.Scope current = getDefinition(
                scope
        );

        logger.info("Checking {} {} scope {} for updates", client.getRealm(), client.getClientId(), scopeDefinition.getName());

        // The type is the only updatable property
        if (isDirty(current, scopeDefinition)) {

            logger.info("Updating {} {} scope {}", client.getRealm(), client.getClientId(), scopeDefinition.getName());

            keycloakRestApi.deleteDefaultScope(client.getRealm(), client.getId(), scope.getId());
            keycloakRestApi.deleteOptionalScope(client.getRealm(), client.getId(), scope.getId());

            switch(scopeDefinition.getType()) {
                case "default": {
                    keycloakRestApi.addDefaultScope(client.getRealm(), client.getId(), scope.getId());
                    break;
                }
                case "optional": {
                    keycloakRestApi.addOptionalScope(client.getRealm(), client.getId(), scope.getId());
                    break;
                }
                default: throw new IllegalStateException(String.format("Note expecting scope type: %s", scopeDefinition.getType()));
            }
            result = true;
        } else {

            logger.info("No Change");
        }

        logger.debug(">updateResource {}", result);
        return result;
    }

    Map<String, ClientDefinition.Scope> getDefinitions(Client client) {
        Map<String, ClientDefinition.Scope> result = new HashMap<>();

        List<Scope> resources = getScopes(client.getRealm(), client.getId());

        for (Scope resource:resources) {

            if (isManaged(resource)) {

                ClientDefinition.Scope definition = getDefinition(resource);
                result.put(resource.getName(), definition);
            }
        }

        return result;
    }

    /*
     * The Keycloak REST API does not return the scope type so you have to implement this wierd logic to discover the type.
     */
    private List<Scope> getScopes(String realmName, String id) {

        List<Scope> clientScopes = keycloakRestApi.getClientScopes(realmName);
        List<String> defaultScopeIds = keycloakRestApi.getDefaultDefaultScopes(realmName, id).stream().map(DefaultScope::getId).toList();
        List<String> optionalScopeIds = keycloakRestApi.getDefaultOptionalScopes(realmName, id).stream().map(DefaultScope::getId).toList();

        clientScopes.forEach(t -> {
            if (defaultScopeIds.contains(t.getId())) {
                t.setType("default");
            } else if (optionalScopeIds.contains(t.getId())) {
                t.setType("optional");
            } else {
                t.setType("none");
            }
        });

        return clientScopes;
    }

    private ClientDefinition.Scope getDefinition(
            Scope resource
    ) {
        ClientDefinition.Scope result = new ClientDefinition.Scope();

        result.setName(resource.getName());
        result.setType(resource.getType());

        return result;
    }

    private boolean isDirty(
            ClientDefinition.Scope current,
            ClientDefinition.Scope updated
    ) {
        return !current.isUnchanged(updated, null, logger);
    }

    private static Map<String, Scope> getRemovedResourceMap(Map<String, Scope> existingResourceMap, List<String> referencedKeys) {
        Map<String, Scope> result = new HashMap<>();

        for (String key: existingResourceMap.keySet()) {

            // If the resource is not referenced
            if (!referencedKeys.contains(key)) {

                result.put(key, existingResourceMap.get(key));
            }
        }

        return result;
    }

    private static List<String> getReferencedResourceKeys(Set<Scope> referencedResources) {
        return referencedResources.stream().map(it -> getKey(it.getRealm(), it.getName())).toList();
    }

    private static String getKey(
            String realmName,
            String scopeName
    ) {
        return String.format("%s:%s", realmName, scopeName).toUpperCase();
    }
}
