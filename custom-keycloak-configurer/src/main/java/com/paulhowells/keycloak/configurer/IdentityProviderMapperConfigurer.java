package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.IdentityProviderMapperDefinition;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.IdentityProviderMapper;
import org.slf4j.Logger;

import java.util.*;

public class IdentityProviderMapperConfigurer {
    private static final String MANAGED_BY_ATTRIBUTE = "managed-by";
    private static final String MANAGED_BY_ATTRIBUTE_VALUE = "idp-mapper-configurer";

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    IdentityProviderMapperConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
    }

    boolean applyIdentityProviderMappers(
            String realmName,
            String idpAlias,
            List<IdentityProviderMapperDefinition> definitions
    ) {

        List<IdentityProviderMapperDefinition> insertDefinitions = new ArrayList<>();
        Map<String, IdentityProviderMapperDefinition> updateDefinitionMap = new HashMap<>();
        Set<IdentityProviderMapper> referencedResources = new HashSet<>();
        Map<String, IdentityProviderMapper> existingResourceMap = getResourceMap(realmName, idpAlias);

        // Identify if the resource is new or if a resource needs to be updated
        // Keep track of which resource are in use, so we know which ones need to be deleted later
        identifyUpdates(
                existingResourceMap,
                definitions,
                insertDefinitions,
                updateDefinitionMap,
                referencedResources
        );

        List<String> referencedResourceKeys = IdentityProviderMapperConfigurer.getReferencedResourceKeys(referencedResources);
        Map<String, IdentityProviderMapper> removedResourceMap = IdentityProviderMapperConfigurer.getRemovedResourceMap(existingResourceMap, referencedResourceKeys);

        return processUpdates(
                realmName,
                idpAlias,
                removedResourceMap,
                insertDefinitions,
                updateDefinitionMap
        );
    }

    private Map<String, IdentityProviderMapper> getResourceMap(
            String realmName,
            String idpAlias
    ) {
        Map<String, IdentityProviderMapper> results = new HashMap<>();

        List<IdentityProviderMapper> resources = keycloakRestApi.getIdentityProviderMappers(realmName, idpAlias);

        for (IdentityProviderMapper resource:resources) {

            String key = getKey(resource.getName());

            results.put(key, resource);
        }

        return results;
    }

    private void identifyUpdates(
            Map<String, IdentityProviderMapper> existingResourceMap,
            List<IdentityProviderMapperDefinition> executionDefinitions,
            List<IdentityProviderMapperDefinition> insertDefinitions,
            Map<String, IdentityProviderMapperDefinition> updateDefinitionsPkMap,
            Set<IdentityProviderMapper> referencedResources
    ) {

        // Identify if the definition is new or if an existing definition needs to be updated
        // Keep track of which definitions are in use, so we know which ones need to be deleted later
        for (IdentityProviderMapperDefinition definition: executionDefinitions) {
            
            String key = getKey(definition.getName());

            IdentityProviderMapper existingResource = existingResourceMap.get(key);
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
            String realmName,
            String idpAlias,
            Map<String, IdentityProviderMapper> removedResourceMap,
            List<IdentityProviderMapperDefinition> insertDefinitions,
            Map<String, IdentityProviderMapperDefinition> updateDefinitionMap
    ) {
        boolean result = false;

        // Delete the obsolete
        for (String key:removedResourceMap.keySet()) {

            IdentityProviderMapper resource = removedResourceMap.get(key);

            result = removeResource(realmName, idpAlias, resource) || result;
        }

        // Insert the new  Flows
        for (IdentityProviderMapperDefinition definition:insertDefinitions) {

            createResource(
                    realmName,
                    idpAlias,
                    definition
            );
            result = true;
        }

        // Update the existing  Flows
        for (IdentityProviderMapperDefinition definition:updateDefinitionMap.values()) {

            result = updateResource(
                    realmName,
                    idpAlias,
                    definition
            ) || result;
        }

        return result;
    }    
    
    private boolean removeResource(
            String realmName,
            String idpAlias,
            IdentityProviderMapper resource
    ) {
        logger.debug("<removeResource");

        logger.warn("Deleting {} IDP mapper {} {}", realmName, idpAlias, resource.getName());

        keycloakRestApi.deleteIdentityProviderMapper(realmName, idpAlias, resource.getId());

        logger.debug(">removeResource");
        return true;
    }

    private void createResource(
            String realmName,
            String idpAlias,
            IdentityProviderMapperDefinition definition
    ) {
        logger.debug("<createResource");

        logger.info("Creating {} IDP mapper {} {}", realmName, idpAlias, definition.getName());

        IdentityProviderMapper resource = new IdentityProviderMapper();

        // Apply changes to resource
        applyDefinition(
                definition,
                resource
        );

        resource.setIdentityProviderAlias(idpAlias);

        // Tag the resource as managed by the Keycloak Configurer
        resource.getConfig().put(MANAGED_BY_ATTRIBUTE, MANAGED_BY_ATTRIBUTE_VALUE);

        String id = keycloakRestApi.addIdentityProviderMapper(realmName, resource);

        resource = keycloakRestApi.getIdentityProviderMapper(realmName, idpAlias, id);

        logger.debug(">createResource {}", resource);
    }

    private boolean updateResource(
            String realmName,
            String idpAlias,
            IdentityProviderMapperDefinition definition
    ) {
        logger.debug("<updateResource");
        boolean result = false;

        IdentityProviderMapper resource = keycloakRestApi.getIdentityProviderMapper(realmName, idpAlias, definition.getId());

        IdentityProviderMapperDefinition current = getDefinition(
                resource
        );

        logger.info("Checking {} IDP Mapper {} {} for updates", realmName, idpAlias, resource.getName());

        if (isDirty(current, definition)) {

            logger.info("Updating {} {} {}", realmName, idpAlias, resource.getName());

            // Apply changes to resource
            applyDefinition(
                    definition,
                    resource
            );

            resource.setIdentityProviderAlias(idpAlias);

            // Tag the resource as managed by the Configurer
            resource.getConfig().put(MANAGED_BY_ATTRIBUTE, MANAGED_BY_ATTRIBUTE_VALUE);

            keycloakRestApi.updateIdentityProviderMapper(
                    realmName,
                    idpAlias,
                    definition.getId(),
                    resource
            );
            result = true;
        } else {

            logger.info("No Change");
        }

        logger.debug(">updateResource {}", result);
        return result;
    }

    List<IdentityProviderMapperDefinition> getDefinitions(List<IdentityProviderMapper> mappers) {
         return mappers.stream().map(this::getDefinition).toList();
    }

    private IdentityProviderMapperDefinition getDefinition(IdentityProviderMapper resource) {
        IdentityProviderMapperDefinition definition = new IdentityProviderMapperDefinition();

        definition.setName(resource.getName());
        definition.setIdentityProviderMapper(resource.getIdentityProviderMapper());
        definition.getConfig().putAll(resource.getConfig());

        return definition;
    }

    private void applyDefinition(
            IdentityProviderMapperDefinition definition,
            IdentityProviderMapper resource
    ) {
        resource.setName(definition.getName());
        resource.setIdentityProviderMapper(definition.getIdentityProviderMapper());
        resource.getConfig().putAll(definition.getConfig());
    }

    private boolean isDirty(
            IdentityProviderMapperDefinition current,
            IdentityProviderMapperDefinition updated
    ) {
        return !current.isUnchanged(updated, null, logger);
    }

    private static Map<String, IdentityProviderMapper> getRemovedResourceMap(Map<String, IdentityProviderMapper> existingResourceMap, List<String> referencedKeys) {
        Map<String, IdentityProviderMapper> result = new HashMap<>();

        for (String key: existingResourceMap.keySet()) {

            // If the resource is not referenced
            if (!referencedKeys.contains(key)) {

                result.put(key, existingResourceMap.get(key));
            }
        }

        return result;
    }

    private static List<String> getReferencedResourceKeys(Set<IdentityProviderMapper> referencedResources) {
        return referencedResources.stream().map(it -> getKey(it.getName())).toList();
    }

    private static String getKey(
            String name
    ) {
        return String.format("%s", name).toUpperCase();
    }
}
