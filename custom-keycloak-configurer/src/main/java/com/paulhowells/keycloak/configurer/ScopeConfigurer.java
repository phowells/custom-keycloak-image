package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.RealmDefinition;
import com.paulhowells.keycloak.configurer.model.ScopeDefinition;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.*;
import org.slf4j.Logger;

import java.util.*;

public class ScopeConfigurer {
    private static final String MANAGED_BY_ATTRIBUTE = "managed-by";
    private static final String MANAGED_BY_ATTRIBUTE_VALUE = "scope-configurer";

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    private final ProtocolMapperConfigurer protocolMapperConfigurer;

    ScopeConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
        this.protocolMapperConfigurer = new ProtocolMapperConfigurer(keycloakRestApi, logger);
    }

    boolean applyScopes(
            Realm realm,
            RealmDefinition realmDefinition
    ) {

        List<ScopeDefinition> insertDefinitions = new ArrayList<>();
        Map<String, ScopeDefinition> updateDefinitionMap = new HashMap<>();
        Set<Scope> referencedResources = new HashSet<>();
        Map<String, Scope> existingResourceMap = getResourceMap(realm);

        // Identify if the resource is new or if a resource needs to be updated
        // Keep track of which resource are in use, so we know which ones need to be deleted later
        identifyUpdates(
                existingResourceMap,
                realmDefinition,
                insertDefinitions,
                updateDefinitionMap,
                referencedResources
        );

        List<String> referencedResourceKeys = getReferencedResourceKeys(referencedResources);
        Map<String, Scope> removedResourceMap = getRemovedResourceMap(existingResourceMap, referencedResourceKeys);

        return processUpdates(
                realm,
                removedResourceMap,
                insertDefinitions,
                updateDefinitionMap
        );
    }

    private Map<String, Scope> getResourceMap(
            Realm realm
    ) {
        Map<String, Scope> results = new HashMap<>();

        String realmName = realm.getRealm();

        List<Scope> scopes = getScopes(realmName);

        for (Scope scope:scopes) {

            String scopeKey = getKey(realmName, scope.getName());

            results.put(scopeKey, scope);
        }

        return results;
    }

    private void identifyUpdates(
            Map<String, Scope> existingResourceMap,
            RealmDefinition realmDefinition,
            List<ScopeDefinition> insertDefinitions,
            Map<String, ScopeDefinition> updateDefinitionsPkMap,
            Set<Scope> referencedResources
    ) {

        // Identify if the definition is new or if an existing definition needs to be updated
        // Keep track of which definitions are in use, so we know which ones need to be deleted later
        for (String scopeName: realmDefinition.getScopes().keySet()) {

            ScopeDefinition definition = realmDefinition.getScopes().get(scopeName);

            String key = getKey(realmDefinition.getRealmName(), definition.getName());

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
            Realm realm,
            Map<String, Scope> removedResourceMap,
            List<ScopeDefinition> insertDefinitions,
            Map<String, ScopeDefinition> updateDefinitionMap
    ) {
        boolean result = false;

        // Delete the obsolete  Scopes
        for (String scopeKey:removedResourceMap.keySet()) {

            Scope scope = removedResourceMap.get(scopeKey);

            result = removeResource(
                    scope
            ) || result;
        }

        // Insert the new  Scopes
        for (ScopeDefinition scopeDefinition:insertDefinitions) {

            createResource(
                    realm,
                    scopeDefinition
            );
            result = true;
        }

        // Update the existing  Scopes
        for (ScopeDefinition scopeDefinition:updateDefinitionMap.values()) {

            result = updateResource(
                    realm,
                    scopeDefinition
            ) || result;
        }

        return result;
    }

    private static boolean isManaged(Scope resource) {

        Map<String, String> attributes = resource.getAttributes();
        String mangedByAttribute = attributes.get(MANAGED_BY_ATTRIBUTE);

        return (MANAGED_BY_ATTRIBUTE_VALUE.equals(mangedByAttribute));
    }

    private boolean removeResource(
            Scope scope
    ) {
        logger.debug("<removeResource");
        boolean result = false;

        if (isManaged(scope)) {

            logger.warn("Deleting scope {} {}", scope.getRealm(), scope.getName());

            keycloakRestApi.deleteDefaultDefaultScope(scope.getRealm(), scope.getId());
            keycloakRestApi.deleteDefaultOptionalScope(scope.getRealm(), scope.getId());
            keycloakRestApi.deleteScope(scope.getRealm(), scope.getId());
            result = true;
        } else {

            logger.debug("{} scope {} not managed by {}", scope.getRealm(), scope.getName(), MANAGED_BY_ATTRIBUTE_VALUE);
        }

        logger.debug(">removeResource");
        return result;
    }

    private void createResource(
            Realm realm,
            ScopeDefinition scopeDefinition
    ) {
        logger.debug("<createResource");

        logger.info("Creating {} scope {}", realm.getRealm(), scopeDefinition.getName());

        Scope scope = new Scope();

        // Apply changes to resource
        applyDefinition(
                scopeDefinition,
                scope
        );

        // Tag the resource as managed by the Keycloak Configurer
        scope.getAttributes().put(MANAGED_BY_ATTRIBUTE, MANAGED_BY_ATTRIBUTE_VALUE);

        String id = keycloakRestApi.createScope(realm.getRealm(), scope);

        switch (scopeDefinition.getType()) {
            case "default": {
                keycloakRestApi.addDefaultDefaultScope(realm.getRealm(), id);
                break;
            }
            case "optional": {
                keycloakRestApi.addDefaultOptionalScope(realm.getRealm(), id);
                break;
            }
        }

        scope = getScope(realm.getRealm(), id);

        protocolMapperConfigurer.applyMappers(
                scope,
                scopeDefinition
        );

        logger.debug(">createResource {}", scope);
    }

    private boolean updateResource(
            Realm realm,
            ScopeDefinition scopeDefinition
    ) {
        logger.debug("<updateResource");
        boolean result = false;

        Scope scope = getScope(
                realm.getRealm(),
                scopeDefinition.getId()
        );

        ScopeDefinition current = getDefinition(
                scope
        );

        logger.info("Checking {} scope {} for updates", realm.getRealm(), scopeDefinition.getName());

        if (isDirty(current, scopeDefinition)) {

            logger.info("Updating {} scope {}", realm.getRealm(), scopeDefinition.getName());

            // Apply changes to resource
            applyDefinition(
                    scopeDefinition,
                    scope
            );

            // Tag the resource as managed by the Keycloak Configurer
            scope.getAttributes().put(MANAGED_BY_ATTRIBUTE, MANAGED_BY_ATTRIBUTE_VALUE);

            keycloakRestApi.updateScope(
                    realm.getRealm(),
                    scope.getId(),
                    scope
            );
            result = true;

            // Delete then add
            keycloakRestApi.deleteDefaultDefaultScope(realm.getRealm(), scope.getId());
            keycloakRestApi.deleteDefaultOptionalScope(realm.getRealm(), scope.getId());

            switch (scopeDefinition.getType()) {
                case "default": {
                    keycloakRestApi.addDefaultDefaultScope(realm.getRealm(), scope.getId());
                    break;
                }
                case "optional": {
                    keycloakRestApi.addDefaultOptionalScope(realm.getRealm(), scope.getId());
                    break;
                }
            }

            scope = getScope(
                    realm.getRealm(),
                    scopeDefinition.getId()
            );

            protocolMapperConfigurer.applyMappers(
                    scope,
                    scopeDefinition
            );

        } else {

            logger.info("No Change");
        }

        logger.debug(">updateResource {}", result);
        return result;
    }

    Map<String, ScopeDefinition> getDefinitions(Realm realm) {
        Map<String, ScopeDefinition> result = new HashMap<>();

        List<Scope> resources = getScopes(realm.getRealm());

        for (Scope resource:resources) {

            if (isManaged(resource)) {

                ScopeDefinition definition = getDefinition(resource);
                result.put(resource.getName(), definition);
            }
        }

        return result;
    }

    /*
     * The Keycloak REST API does not return the scope type so you have to implement this wierd logic to discover the type.
     */
    private List<Scope> getScopes(String realmName) {

        List<Scope> clientScopes = keycloakRestApi.getClientScopes(realmName);
        List<String> defaultScopeIds = keycloakRestApi.getDefaultDefaultScopes(realmName).stream().map(DefaultScope::getId).toList();
        List<String> optionalScopeIds = keycloakRestApi.getDefaultOptionalScopes(realmName).stream().map(DefaultScope::getId).toList();

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
    private Scope getScope(String realmName, String id) {

        Scope clientScope = keycloakRestApi.getScope(realmName, id);
        List<String> defaultScopeIds = keycloakRestApi.getDefaultDefaultScopes(realmName).stream().map(DefaultScope::getId).toList();
        List<String> optionalScopeIds = keycloakRestApi.getDefaultOptionalScopes(realmName).stream().map(DefaultScope::getId).toList();

        if (defaultScopeIds.contains(clientScope.getId())) {
            clientScope.setType("default");
        } else if (optionalScopeIds.contains(clientScope.getId())) {
            clientScope.setType("optional");
        } else {
            clientScope.setType("none");
        }

        return clientScope;
    }

    private ScopeDefinition getDefinition(
            Scope resource
    ) {
        ScopeDefinition result = new ScopeDefinition();

        result.setName(resource.getName());
        result.setDescription(resource.getDescription());
        result.setType(resource.getType());
        result.setProtocol(resource.getProtocol());
        Map<String, String> attributes = resource.getAttributes();
        result.setIncludeInTokenScope(Boolean.valueOf(attributes.get("include.in.token.scope")));
        result.setDisplayOnConsentScreen(Boolean.valueOf(attributes.get("display.on.consent.screen")));
        result.setConsentScreenText(attributes.get("consent.screen.text"));
        result.setManaged(isManaged(resource));

        return result;
    }

    private void applyDefinition(
            ScopeDefinition definition,
            Scope resource
    ) {
        resource.setName(definition.getName());
        resource.setDescription(definition.getDescription());
        resource.setType(definition.getType());
        resource.setProtocol(definition.getProtocol());
        resource.getAttributes().put("include.in.token.scope", definition.getIncludeInTokenScope()==null?null:definition.getIncludeInTokenScope().toString());
        resource.getAttributes().put("display.on.consent.screen", definition.getDisplayOnConsentScreen()==null?null:definition.getDisplayOnConsentScreen().toString());
        resource.getAttributes().put("consent.screen.text", definition.getConsentScreenText());
    }

    private boolean isDirty(
            ScopeDefinition current,
            ScopeDefinition updated
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
