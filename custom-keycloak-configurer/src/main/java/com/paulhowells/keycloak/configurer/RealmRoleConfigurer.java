package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.RealmDefinition;
import com.paulhowells.keycloak.configurer.model.RoleDefinition;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.Role;
import com.paulhowells.keycloak.configurer.rest.client.model.Realm;
import org.slf4j.Logger;

import java.util.*;


public class RealmRoleConfigurer {
    private static final String MANAGED_BY_ATTRIBUTE = "managed-by";
    private static final String MANAGED_BY_ATTRIBUTE_VALUE = "role-configurer";
    private static final List<String> MANAGED_BY_ATTRIBUTE_VALUES;

    static {
        MANAGED_BY_ATTRIBUTE_VALUES = new ArrayList<>();
        MANAGED_BY_ATTRIBUTE_VALUES.add(MANAGED_BY_ATTRIBUTE_VALUE);
    }

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    RealmRoleConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
    }

    boolean applyRoles(
            Realm realm,
            RealmDefinition realmDefinition
    ) {

        List<RoleDefinition> insertDefinitions = new ArrayList<>();
        Map<String, RoleDefinition> updateDefinitionMap = new HashMap<>();
        Set<Role> referencedResources = new HashSet<>();
        Map<String, Role> existingResourceMap = getResourceMap(realm);

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
        Map<String, Role> removedResourceMap = getRemovedResourceMap(existingResourceMap, referencedResourceKeys);

        return processUpdates(
                realm,
                removedResourceMap,
                insertDefinitions,
                updateDefinitionMap
        );
    }

    private Map<String, Role> getResourceMap(
            Realm realm
    ) {
        Map<String, Role> results = new HashMap<>();

        String realmName = realm.getRealm();

        int pageOffset = 0;
        int pageSize = 10;
        int pageCount;
        do {
            List<Role> roles = keycloakRestApi.getRoles(realmName, pageOffset, pageOffset + pageSize);

            for (Role role:roles) {

                String roleKey = getKey(realmName, role.getName());

                results.put(roleKey, role);
            }

            pageCount = roles.size();
            pageOffset += pageCount;
        } while (pageCount >= pageSize);

        return results;
    }

    private void identifyUpdates(
            Map<String, Role> existingResourceMap,
            RealmDefinition realmDefinition,
            List<RoleDefinition> insertDefinitions,
            Map<String, RoleDefinition> updateDefinitionsPkMap,
            Set<Role> referencedResources
    ) {

        // Identify if the definition is new or if an existing definition needs to be updated
        // Keep track of which definitions are in use, so we know which ones need to be deleted later
        for (String roleName: realmDefinition.getRoles().keySet()) {

            RoleDefinition definition = realmDefinition.getRoles().get(roleName);

            String key = getKey(realmDefinition.getRealmName(), definition.getName());

            Role existingResource = existingResourceMap.get(key);
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
            Map<String, Role> removedResourceMap,
            List<RoleDefinition> insertDefinitions,
            Map<String, RoleDefinition> updateDefinitionMap
    ) {
        boolean result = false;

        // Delete the obsolete  Roles
        for (String roleKey:removedResourceMap.keySet()) {

            Role role = removedResourceMap.get(roleKey);

            result = removeResource(
                    role
            ) || result;
        }

        // Insert the new  Roles
        for (RoleDefinition roleDefinition:insertDefinitions) {

            createResource(
                    realm,
                    roleDefinition
            );
            result = true;
        }

        // Update the existing  Roles
        for (RoleDefinition roleDefinition:updateDefinitionMap.values()) {

            result = updateResource(
                    realm,
                    roleDefinition
            ) || result;
        }

        return result;
    }

    private static boolean isManaged(Role resource) {

        Map<String, List<String>> attributes = resource.getAttributes();
        List<String> mangedByAttribute = attributes.get(MANAGED_BY_ATTRIBUTE);

        return (mangedByAttribute != null && mangedByAttribute.contains(MANAGED_BY_ATTRIBUTE_VALUE));
    }

    private boolean removeResource(
            Role role
    ) {
        logger.debug("<removeResource");
        boolean result = false;

        if (isManaged(role)) {

            logger.warn("Deleting role {} {}", role.getRealm(), role.getName());

            keycloakRestApi.deleteRole(role.getRealm(), role.getId());
            result = true;
        } else {

            logger.debug("{} role {} not managed by {}", role.getRealm(), role.getName(), MANAGED_BY_ATTRIBUTE_VALUE);
        }

        logger.debug(">removeResource");
        return result;
    }

    private void createResource(
            Realm realm,
            RoleDefinition roleDefinition
    ) {
        logger.debug("<createResource");

        logger.info("Creating {} role {}", realm.getRealm(), roleDefinition.getName());

        Role role = new Role();

        // Apply changes to resource
        applyDefinition(
                roleDefinition,
                role
        );

        // Tag the resource as managed by the Keycloak Configurer
        role.getAttributes().put(MANAGED_BY_ATTRIBUTE, MANAGED_BY_ATTRIBUTE_VALUES);

        String id = keycloakRestApi.createRole(realm.getRealm(), role);

        role = keycloakRestApi.getRole(realm.getRealm(), id);
        logger.debug(">createResource {}", role);
    }

    private boolean updateResource(
            Realm realm,
            RoleDefinition roleDefinition
    ) {
        logger.debug("<updateResource");
        boolean result = false;

        Role role = keycloakRestApi.getRole(
                realm.getRealm(),
                roleDefinition.getId()
        );

        RoleDefinition current = getDefinition(
                role
        );

        logger.info("Checking {} role {} for updates", realm.getRealm(), roleDefinition.getName());

        if (isDirty(current, roleDefinition)) {

            logger.info("Updating {} role {}", realm.getRealm(), roleDefinition.getName());

            // Apply changes to resource
            applyDefinition(
                    roleDefinition,
                    role
            );

            // Tag the resource as managed by the Keycloak Configurer
            role.getAttributes().put(MANAGED_BY_ATTRIBUTE, MANAGED_BY_ATTRIBUTE_VALUES);

            keycloakRestApi.updateRole(
                    realm.getRealm(),
                    roleDefinition.getId(),
                    role
            );
            result = true;
        } else {

            logger.info("No Change");
        }

        logger.debug(">updateResource {}", result);
        return result;
    }

    Map<String, RoleDefinition> getDefinitions(Realm realm) {
        Map<String, RoleDefinition> result = new HashMap<>();

        int pageOffset = 0;
        int pageSize = 10;
        int pageCount;
        do {
            List<Role> resources = keycloakRestApi.getRoles(realm.getRealm(), pageOffset, pageOffset + pageSize);

            for (Role resource:resources) {

                if(isManaged(resource)) {

                    RoleDefinition definition = getDefinition(resource);
                    result.put(resource.getName(), definition);
                }
            }

            pageCount = resources.size();
            pageOffset += pageCount;
        } while (pageCount >= pageSize);

        return result;
    }

    private RoleDefinition getDefinition(
            Role resource
    ) {
        RoleDefinition result = new RoleDefinition();

        result.setName(resource.getName());
        result.setDescription(resource.getDescription());
        result.setManaged(isManaged(resource));

        return result;
    }

    private void applyDefinition(
            RoleDefinition definition,
            Role resource
    ) {
        resource.setName(definition.getName());
        resource.setDescription(definition.getDescription());
    }

    private boolean isDirty(
            RoleDefinition current,
            RoleDefinition updated
    ) {
        return !current.isUnchanged(updated, null, logger);
    }

    private static Map<String, Role> getRemovedResourceMap(Map<String, Role> existingResourceMap, List<String> referencedKeys) {
        Map<String, Role> result = new HashMap<>();

        for (String key: existingResourceMap.keySet()) {

            // If the resource is not referenced
            if (!referencedKeys.contains(key)) {

                result.put(key, existingResourceMap.get(key));
            }
        }

        return result;
    }

    private static List<String> getReferencedResourceKeys(Set<Role> referencedResources) {
        return referencedResources.stream().map(it -> getKey(it.getRealm(), it.getName())).toList();
    }

    private static String getKey(
            String realmName,
            String roleName
    ) {
        return String.format("%s:%s", realmName, roleName).toUpperCase();
    }
}
