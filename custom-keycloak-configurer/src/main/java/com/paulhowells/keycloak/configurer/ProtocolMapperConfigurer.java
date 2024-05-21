package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.ScopeDefinition;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.ProtocolMapper;
import com.paulhowells.keycloak.configurer.rest.client.model.Scope;
import org.slf4j.Logger;

import java.util.*;

public class ProtocolMapperConfigurer {
    private static final String MANAGED_BY_ATTRIBUTE = "managed-by";
    private static final String MANAGED_BY_ATTRIBUTE_VALUE = "protocol-mapper-configurer";
    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    ProtocolMapperConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
    }

    boolean applyMappers(
            Scope scope,
            ScopeDefinition scopeDefinition
    ) {

        List<ScopeDefinition.Mapper> insertDefinitions = new ArrayList<>();
        Map<String, ScopeDefinition.Mapper> updateDefinitionMap = new HashMap<>();
        Set<ProtocolMapper> referencedResources = new HashSet<>();
        Map<String, ProtocolMapper> existingResourceMap = getResourceMap(scope);

        // Identify if the resource is new or if a resource needs to be updated
        // Keep track of which resource are in use, so we know which ones need to be deleted later
        identifyUpdates(
                existingResourceMap,
                scopeDefinition,
                insertDefinitions,
                updateDefinitionMap,
                referencedResources
        );

        List<String> referencedResourceKeys = getReferencedResourceKeys(referencedResources);
        Map<String, ProtocolMapper> removedResourceMap = getRemovedResourceMap(existingResourceMap, referencedResourceKeys);

        return processUpdates(
                scope,
                removedResourceMap,
                insertDefinitions,
                updateDefinitionMap
        );
    }

    private Map<String, ProtocolMapper> getResourceMap(
            Scope scope
    ) {
        Map<String, ProtocolMapper> results = new HashMap<>();

        String realmName = scope.getRealm();

        List<ProtocolMapper> mappers = getMappers(realmName, scope.getId());

        for (ProtocolMapper mapper:mappers) {

            String key = getKey(mapper.getName());

            results.put(key, mapper);
        }

        return results;
    }

    private void identifyUpdates(
            Map<String, ProtocolMapper> existingResourceMap,
            ScopeDefinition scopeDefinition,
            List<ScopeDefinition.Mapper> insertDefinitions,
            Map<String, ScopeDefinition.Mapper> updateDefinitionsPkMap,
            Set<ProtocolMapper> referencedResources
    ) {

        // Identify if the definition is new or if an existing definition needs to be updated
        // Keep track of which definitions are in use, so we know which ones need to be deleted later
        for (String mapperName: scopeDefinition.getMappers().keySet()) {

            ScopeDefinition.Mapper definition = scopeDefinition.getMappers().get(mapperName);

            String key = getKey(definition.getName());

            ProtocolMapper existingResource = existingResourceMap.get(key);
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
            Scope scope,
            Map<String, ProtocolMapper> removedResourceMap,
            List<ScopeDefinition.Mapper> insertDefinitions,
            Map<String, ScopeDefinition.Mapper> updateDefinitionMap
    ) {
        boolean result = false;

        // Delete the obsolete resources
        for (String key:removedResourceMap.keySet()) {

            ProtocolMapper mapper = removedResourceMap.get(key);

            result = removeResource(
                    scope,
                    mapper
            ) || result;
        }

        // Insert the new resources
        for (ScopeDefinition.Mapper mapperDefinition:insertDefinitions) {

            createResource(
                    scope,
                    mapperDefinition
            );
            result = true;
        }

        // Update the existing resources
        for (ScopeDefinition.Mapper mapperDefinition:updateDefinitionMap.values()) {

            result = updateResource(
                    scope,
                    mapperDefinition
            ) || result;
        }

        return result;
    }

    private static boolean isManaged(ProtocolMapper resource) {

        Map<String, String> config = resource.getConfig();
        String mangedByAttribute = config.get(MANAGED_BY_ATTRIBUTE);

        return (MANAGED_BY_ATTRIBUTE_VALUE.equals(mangedByAttribute));
    }

    private boolean removeResource(
            Scope scope,
            ProtocolMapper protocolMapper
    ) {
        logger.debug("<removeResource");
        boolean result = false;

        if (isManaged(protocolMapper)) {

            logger.warn("Deleting  {} {} scope mapper {}", scope.getRealm(), scope.getName(), protocolMapper.getName());

            keycloakRestApi.deleteScopeMapper(scope.getRealm(), scope.getId(), protocolMapper.getId());
            result = true;
        }

        logger.debug(">removeResource");
        return result;
    }

    private void createResource(
            Scope scope,
            ScopeDefinition.Mapper mapperDefinition
    ) {
        logger.debug("<createResource");

        logger.info("Creating {} {} scope mapper {}", scope.getRealm(), scope.getName(), mapperDefinition.getName());

        ProtocolMapper protocolMapper = new ProtocolMapper();

        // Apply changes to resource
        applyDefinition(
                mapperDefinition,
                protocolMapper
        );

        // Tag the resource as managed by the Keycloak Configurer
        protocolMapper.getConfig().put(MANAGED_BY_ATTRIBUTE, MANAGED_BY_ATTRIBUTE_VALUE);

        String id = keycloakRestApi.addScopeMapper(scope.getRealm(), scope.getId(), protocolMapper);

        protocolMapper = keycloakRestApi.getScopeMapper(scope.getRealm(), scope.getId(), id);

        logger.debug(">createResource {}", protocolMapper);
    }

    private boolean updateResource(
            Scope scope,
            ScopeDefinition.Mapper mapperDefinition
    ) {
        logger.debug("<updateResource");
        boolean result = false;

        ProtocolMapper protocolMapper = keycloakRestApi.getScopeMapper(scope.getRealm(), scope.getId(), mapperDefinition.getId());

        ScopeDefinition.Mapper current = getDefinition(
                protocolMapper
        );

        logger.info("Checking {} {} scope mapper {} for updates", scope.getRealm(), scope.getName(), mapperDefinition.getName());

        // The type is the only updatable property
        if (isDirty(current, mapperDefinition)) {

            logger.info("Updating {} {} scope mapper {}", scope.getRealm(), scope.getName(), mapperDefinition.getName());

            keycloakRestApi.updateScopeMapper(scope.getRealm(), scope.getId(), protocolMapper.getId(), protocolMapper);
            result = true;
        } else {

            logger.info("No Change");
        }

        logger.debug(">updateResource {}", result);
        return result;
    }

    private Map<String, ScopeDefinition.Mapper> getDefinitions(Scope scope) {
        Map<String, ScopeDefinition.Mapper> result = new HashMap<>();

        List<ProtocolMapper> resources = getMappers(scope.getRealm(), scope.getId());

        for (ProtocolMapper resource:resources) {

            if (isManaged(resource)) {

                ScopeDefinition.Mapper definition = getDefinition(resource);
                result.put(resource.getName(), definition);
            }
        }

        return result;
    }

    private List<ProtocolMapper> getMappers(String realmName, String id) {

        return keycloakRestApi.getScopeMappers(realmName, id);
    }

    private ScopeDefinition.Mapper getDefinition(
            ProtocolMapper resource
    ) {
        ScopeDefinition.Mapper result = new ScopeDefinition.Mapper();

        result.setName(resource.getName());
        result.setProtocol(resource.getProtocol());
        result.setProtocolMapper(resource.getProtocolMapper());
        result.setConsentRequired(Boolean.TRUE.equals(resource.getConsentRequired()));
        result.getConfig().putAll(resource.getConfig());

        return result;
    }

    private void applyDefinition(ScopeDefinition.Mapper definition, ProtocolMapper resource) {

        resource.setName(definition.getName());
        resource.setProtocol(definition.getProtocol());
        resource.setProtocolMapper(definition.getProtocolMapper());
        resource.setConsentRequired(Boolean.TRUE.equals(resource.getConsentRequired()));
        resource.getConfig().clear();
        resource.getConfig().putAll(definition.getConfig());
    }

    private boolean isDirty(
            ScopeDefinition.Mapper current,
            ScopeDefinition.Mapper updated
    ) {
        return !current.isUnchanged(updated, null, logger);
    }

    private static Map<String, ProtocolMapper> getRemovedResourceMap(Map<String, ProtocolMapper> existingResourceMap, List<String> referencedKeys) {
        Map<String, ProtocolMapper> result = new HashMap<>();

        for (String key: existingResourceMap.keySet()) {

            // If the resource is not referenced
            if (!referencedKeys.contains(key)) {

                result.put(key, existingResourceMap.get(key));
            }
        }

        return result;
    }

    private static List<String> getReferencedResourceKeys(Set<ProtocolMapper> referencedResources) {
        return referencedResources.stream().map(it -> getKey(it.getName())).toList();
    }

    private static String getKey(
            String mapperName
    ) {
        return String.format("%s", mapperName).toUpperCase();
    }
}
