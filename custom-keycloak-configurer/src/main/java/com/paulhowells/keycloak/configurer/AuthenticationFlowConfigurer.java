package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.AuthenticationFlowDefinition;
import com.paulhowells.keycloak.configurer.model.KeycloakDefinition;
import com.paulhowells.keycloak.configurer.model.RealmDefinition;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.*;
import com.paulhowells.keycloak.configurer.rest.client.model.Flow;
import org.slf4j.Logger;

import java.util.*;

public class AuthenticationFlowConfigurer {
    
    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    private final AuthenticationFlowExecutionConfigurer authenticationFlowExecutionConfigurer;

    AuthenticationFlowConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
        this.authenticationFlowExecutionConfigurer = new AuthenticationFlowExecutionConfigurer(
                keycloakRestApi,
                logger
        );
    }

    boolean validateDefinitions(
            Map<String, KeycloakDefinition> keycloakDefinitionMap,
            Map<String, Pair<String, AuthenticationFlowDefinition>> definitions
    ) {

        var result = true;

        for (String path:keycloakDefinitionMap.keySet()) {

            KeycloakDefinition keycloakDefinition = keycloakDefinitionMap.get(path);

            for (int index=0;index<keycloakDefinition.getAuthenticationFlows().size();++index) {

                AuthenticationFlowDefinition definition = keycloakDefinition.getAuthenticationFlows().get(index);

                var valid = true;

                String realmName = definition.getRealmName();

                if (realmName == null || realmName.isBlank()) {
                    logger.error("Authentication Flow Definition {} in {} is missing the realmName", index, path);
                    valid = false;
                }

                String alias = definition.getAlias();

                if (alias == null || alias.isBlank()) {
                    logger.error("Authentication Flow Definition {} in {} is missing the alias", index, path);
                    valid = false;
                }

                String key = getKey(realmName, alias);

                if (valid) {

                    Pair<String, AuthenticationFlowDefinition> existingDefinition = definitions.get(key);
                    if (existingDefinition != null) {
                        logger.error("Duplicate Authentication Flow Definitions with alias '{}' in {} and {}", alias, path, existingDefinition.first());
                        valid = false;
                    }
                }

                // TODO Validate properties

                // Does realm exist?
                RealmDefinition realmDefinition = findRealmDefinition(realmName, keycloakDefinitionMap);
                Realm realm = keycloakRestApi.getRealmByName(realmName);

                if (valid) {
                    if (realmDefinition == null) {

                        // If the realm does not exist or will be removed
                        if (realm == null || RealmConfigurer.isManaged(realm)) {

                            logger.error("Authentication Flow Definition {} in {} references the invalid realm '{}'", index, path, realmName);
                            valid = false;
                        }
                    }
                }

                if (valid) {
                    definitions.put(key, new Pair<>(path, definition));
                } else {
                    result = false;
                }
            }
        }

        return result;
    }

    boolean applyAuthenticationFlows(
            KeycloakDefinition keycloakDefinition
    ) {

        List<AuthenticationFlowDefinition> insertAuthenticationFlowDefinitions = new ArrayList<>();
        Map<String, AuthenticationFlowDefinition> updateAuthenticationFlowPkMap = new HashMap<>();
        Set<Flow> referencedAuthenticationFlows = new HashSet<>();
        Map<String, Flow> existingAuthenticationFlowMap = getResourceMap();

        // Identify if the resource is new or if a resource needs to be updated
        // Keep track of which resource are in use, so we know which ones need to be deleted later
        identifyUpdates(
                existingAuthenticationFlowMap,
                keycloakDefinition,
                insertAuthenticationFlowDefinitions,
                updateAuthenticationFlowPkMap,
                referencedAuthenticationFlows
        );

        List<String> referencedAuthenticationFlowKeys = getReferencedResourceKeys(referencedAuthenticationFlows);
        Map<String, Flow> removedAuthenticationFlowMap = getRemovedResourceMap(existingAuthenticationFlowMap, referencedAuthenticationFlowKeys);

        return processUpdates(
                removedAuthenticationFlowMap,
                insertAuthenticationFlowDefinitions,
                updateAuthenticationFlowPkMap
        );
    }

    private RealmDefinition findRealmDefinition(String realmName, Map<String, KeycloakDefinition> keycloakDefinitionMap) {
        RealmDefinition result = null;

        outer: for (KeycloakDefinition keycloakDefinition:keycloakDefinitionMap.values()) {
            for (RealmDefinition realmDefinition:keycloakDefinition.getRealms()) {
                if (realmName.equals(realmDefinition.getRealmName())) {
                    result = realmDefinition;
                    break outer;
                }
            }
        }

        return result;
    }

    private Map<String, Flow> getResourceMap() {
        Map<String, Flow> results = new HashMap<>();

        List<Realm> realms = keycloakRestApi.getRealms();

        for(Realm realm:realms) {

            String realmName = realm.getRealm();

            List<Flow> flows = keycloakRestApi.getTopLevelFlows(realmName);

            for (Flow flow:flows) {

                boolean builtIn = Boolean.TRUE.equals(flow.getBuiltIn());

                if (!builtIn) {

                    String key = getKey(realmName, flow.getAlias());

                    results.put(key, flow);
                }
            }
        }

        return results;
    }

    private void identifyUpdates(
            Map<String, Flow> existingResourceMap,
            KeycloakDefinition keycloakDefinition,
            List<AuthenticationFlowDefinition> insertDefinitions,
            Map<String, AuthenticationFlowDefinition> updateDefinitionsPkMap,
            Set<Flow> referencedResources
    ) {

        // Identify if the definition is new or if an existing definition needs to be updated
        // Keep track of which definitions are in use, so we know which ones need to be deleted later
        for (AuthenticationFlowDefinition definition: keycloakDefinition.getAuthenticationFlows()) {

            String key = getKey(definition.getRealmName(), definition.getAlias());

            Flow existingResource = existingResourceMap.get(key);
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
            Map<String, Flow> removedResourceMap,
            List<AuthenticationFlowDefinition> insertDefinitions,
            Map<String, AuthenticationFlowDefinition> updateDefinitionMap
    ) {
        boolean result = false;

        // Delete the obsolete  Flows
        for (String key:removedResourceMap.keySet()) {

            Flow resource = removedResourceMap.get(key);

            result = removeResource(resource) || result;
        }

        // Insert the new  Flows
        for (AuthenticationFlowDefinition definition:insertDefinitions) {

            createResource(
                    definition
            );
            result = true;
        }

        // Update the existing  Flows
        for (AuthenticationFlowDefinition definition:updateDefinitionMap.values()) {

            result = updateResource(
                    definition
            ) || result;
        }

        return result;
    }

    private static boolean isManaged(Flow resource) {

        return !Boolean.TRUE.equals(resource.getBuiltIn());
    }
    
    private boolean removeResource(
            Flow resource
    ) {
        logger.debug("<removeResource");
        boolean result = false;

        if (isManaged(resource)) {

            // We need to delete the children so that we do not leave behind orphaned records which may cause problems later
            this.authenticationFlowExecutionConfigurer.applyFlowExecutions(resource, new ArrayList<>());

            logger.warn("Deleting flow {} {}", resource.getRealm(), resource.getAlias());

            keycloakRestApi.deleteTopLevelFlow(resource.getRealm(), resource.getId());
            result = true;
        } else {

            logger.debug("{} flow {} is built-in", resource.getRealm(), resource.getAlias());
        }

        logger.debug(">removeResource");
        return result;
    }

    private void createResource(
            AuthenticationFlowDefinition definition
    ) {
        logger.debug("<createResource");

        logger.info("Creating {} authentication flow {}", definition.getRealmName(), definition.getAlias());

        Flow resource = new Flow();

        // Apply changes to resource
        applyDefinition(
                definition,
                resource
        );

        String id = keycloakRestApi.createTopLevelFlow(definition.getRealmName(), resource);

        resource = keycloakRestApi.getTopLevelFlow(definition.getRealmName(), id);

        authenticationFlowExecutionConfigurer.applyFlowExecutions(
                resource,
                definition.getExecutions()
        );

        logger.debug(">createResource {}", resource);
    }        
    
    private boolean updateResource(
            AuthenticationFlowDefinition definition
    ) {
        logger.debug("<updateResource");
        boolean result = false;

        Flow resource = keycloakRestApi.getTopLevelFlow(definition.getRealmName(), definition.getId());

        AuthenticationFlowDefinition current = getDefinition(
                resource
        );

        logger.info("Checking {} authentication flow {} for updates", definition.getRealmName(), definition.getAlias());

        if (isDirty(current, definition)) {

            logger.info("Updating {} authentication flow  {}", definition.getRealmName(), definition.getAlias());

            // Apply changes to resource
            applyDefinition(
                    definition,
                    resource
            );

            keycloakRestApi.updateTopLevelFlow(
                    definition.getRealmName(),
                    definition.getId(),
                    resource
            );

            resource = keycloakRestApi.getTopLevelFlow(definition.getRealmName(), definition.getId());

            authenticationFlowExecutionConfigurer.applyFlowExecutions(
                    resource,
                    definition.getExecutions()
            );

            result = true;
        } else {

            logger.info("No Change");
        }

        logger.debug(">updateResource {}", result);
        return result;
    }

    List<AuthenticationFlowDefinition> getDefinitions(Realm realm) {
        List<AuthenticationFlowDefinition> results = new ArrayList<>();

        for (Flow resource:keycloakRestApi.getTopLevelFlows(realm.getRealm())) {

            if (isManaged(resource)) {

                AuthenticationFlowDefinition definition = getDefinition(resource);
                results.add(definition);
            }
        }

        return results;
    }

    private AuthenticationFlowDefinition getDefinition(
            Flow flow
    ) {
        AuthenticationFlowDefinition flowDefinition = new AuthenticationFlowDefinition();

        flowDefinition.setRealmName(flow.getRealm());

        flowDefinition.setId(flow.getId());
        flowDefinition.setAlias(flow.getAlias());
        flowDefinition.setDescription(flow.getDescription());
        flowDefinition.setProviderId(flow.getProviderId());

        List<AuthenticationFlowDefinition.Execution> executions = this.authenticationFlowExecutionConfigurer.getExecutionDefinitions(flow);
        flowDefinition.getExecutions().addAll(executions);

        // Ensure that the executions are in the correct order
        flowDefinition.getExecutions().sort(Comparator.comparingInt(AuthenticationFlowDefinition.Execution::getIndex));

        return flowDefinition;
    }

    private void applyDefinition(
            AuthenticationFlowDefinition definition,
            Flow resource
    ) {

        resource.setAlias(definition.getAlias());
        resource.setDescription(definition.getDescription());
        resource.setProviderId(definition.getProviderId());
    }
    
    private boolean isDirty(
            AuthenticationFlowDefinition current,
            AuthenticationFlowDefinition updated
    ) {
        return !current.isUnchanged(updated, "authenticationFlow", logger);
    }

    private static Map<String, Flow> getRemovedResourceMap(Map<String, Flow> existingResourceMap, List<String> referencedKeys) {
        Map<String, Flow> result = new HashMap<>();

        for (String key: existingResourceMap.keySet()) {

            // If the resource is not referenced
            if (!referencedKeys.contains(key)) {

                result.put(key, existingResourceMap.get(key));
            }
        }

        return result;
    }

    private static List<String> getReferencedResourceKeys(Set<Flow> referencedResources) {
        return referencedResources.stream().map(it -> getKey(it.getRealm(), it.getAlias())).toList();
    }

    private static String getKey(
            String realmName,
            String alias
    ) {
        return String.format("%s:%s", realmName, alias).toUpperCase();
    }
}
