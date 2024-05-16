package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.RealmDefinition;
import com.paulhowells.keycloak.configurer.model.RealmDefinition.DefaultRole;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.Client;
import com.paulhowells.keycloak.configurer.rest.client.model.Realm;
import com.paulhowells.keycloak.configurer.rest.client.model.Role;
import org.slf4j.Logger;

import java.util.*;


public class DefaultRoleConfigurer {

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    DefaultRoleConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
    }

    boolean applyRoles(
            Realm realm,
            RealmDefinition realmDefinition
    ) {

        List<RealmDefinition.DefaultRole> insertDefinitions = new ArrayList<>();
        Set<Role> referencedResources = new HashSet<>();
        Map<String, Role> existingResourceMap = getResourceMap(realm);

        // Identify if the resource is new or if a resource needs to be updated
        // Keep track of which resource are in use, so we know which ones need to be deleted later
        identifyUpdates(
                existingResourceMap,
                realmDefinition,
                insertDefinitions,
                referencedResources
        );

        List<String> referencedResourceKeys = getReferencedResourceKeys(referencedResources);
        Map<String, Role> removedResourceMap = getRemovedResourceMap(existingResourceMap, referencedResourceKeys);

        return processUpdates(
                realm,
                removedResourceMap,
                insertDefinitions
        );
    }

    private Map<String, Role> getResourceMap(
            Realm realm
    ) {
        Map<String, Role> results = new HashMap<>();

        String realmName = realm.getRealm();
        String defaultRoleId = realm.getDefaultRole().getId();
        
        List<Role> roles = keycloakRestApi.getChildRoles(realmName, defaultRoleId);

        for (Role role:roles) {

            if (Boolean.TRUE.equals(role.getClientRole())) {

                Client client = keycloakRestApi.getClient(realmName, role.getContainerId());

                role.setClientId(client.getClientId());
            }

            String roleKey = getKey(role.getClientId(), role.getName());

            results.put(roleKey, role);
        }

        return results;
    }

    private void identifyUpdates(
            Map<String, Role> existingResourceMap,
            RealmDefinition realmDefinition,
            List<RealmDefinition.DefaultRole> insertDefinitions,
            Set<Role> referencedResources
    ) {

        // Identify if the definition is new or if an existing definition needs to be updated
        // Keep track of which definitions are in use, so we know which ones need to be deleted later
        for (String roleName: realmDefinition.getDefaultRoles().keySet()) {

            RealmDefinition.DefaultRole definition = realmDefinition.getDefaultRoles().get(roleName);

            String key = getKey(definition.getClientId(), definition.getName());

            Role existingResource = existingResourceMap.get(key);
            if (existingResource == null) {

                insertDefinitions.add(definition);
            } else {

                referencedResources.add(existingResource);
            }
        }
    }

    private boolean processUpdates(
            Realm realm,
            Map<String, Role> removedResourceMap,
            List<RealmDefinition.DefaultRole> insertDefinitions
    ) {
        boolean result = false;

        Map<String, Role> roleMap = getRoleMap(realm);

        // Delete the obsolete  Roles
        for (String roleKey:removedResourceMap.keySet()) {

            Role role = removedResourceMap.get(roleKey);

            result = removeResource(
                    realm,
                    role
            ) || result;
        }

        // Insert the new  Roles
        for (RealmDefinition.DefaultRole definition:insertDefinitions) {

            createResource(
                    realm,
                    definition,
                    roleMap
            );
            result = true;
        }

        return result;
    }

    private static String toString(String clientId, String name) {

        if (clientId !=null && !clientId.isBlank()) {
            return String.format("%s %s", clientId, name);
        } else {
            return name;
        }
    }

    private boolean removeResource(
            Realm realm,
            Role role
    ) {
        logger.debug("<removeResource");
        boolean result = true;

        logger.warn("Removing {} default role {}", realm.getRealm(), toString(role.getClientId(), role.getName()));

        keycloakRestApi.removeChildRole(realm.getRealm(), realm.getDefaultRole().getId(), role.getId());

        logger.debug(">removeResource");
        return result;
    }

    private void createResource(
            Realm realm,
            RealmDefinition.DefaultRole definition,
            Map<String, Role> roleMap
    ) {
        logger.debug("<createResource");

        logger.info("Adding {} default role {}", realm.getRealm(), toString(definition.getClientId(), definition.getName()));

        Role role = roleMap.get(getKey(definition));

        String id = keycloakRestApi.addChildRole(realm.getRealm(), realm.getDefaultRole().getId(), role.getId());

        role = keycloakRestApi.getRole(realm.getRealm(), id);
        logger.debug(">createResource {}", role);
    }

    Map<String, RealmDefinition.DefaultRole> getDefinitions(Realm realm) {
        Map<String, RealmDefinition.DefaultRole> result = new HashMap<>();

        int pageOffset = 0;
        int pageSize = 10;
        int pageCount;
        do {
            List<Role> resources = keycloakRestApi.getRoles(realm.getRealm(), pageOffset, pageOffset + pageSize);

            for (Role resource:resources) {

                RealmDefinition.DefaultRole definition = getDefinition(resource);
                result.put(resource.getName(), definition);
            }

            pageCount = resources.size();
            pageOffset += pageCount;
        } while (pageCount >= pageSize);

        return result;
    }

    private RealmDefinition.DefaultRole getDefinition(
            Role resource
    ) {
        RealmDefinition.DefaultRole result = new RealmDefinition.DefaultRole();

        result.setName(resource.getName());

        if (Boolean.TRUE.equals(resource.getClientRole())) {

            Client client = keycloakRestApi.getClient(resource.getRealm(), resource.getContainerId());
            result.setClientId(client.getClientId());
        }

        return result;
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
        return referencedResources.stream().map(it -> getKey(it.getClientId(), it.getName())).toList();
    }

    private static String getKey(
            String clientId,
            String roleName
    ) {
        return String.format("%s:%s", clientId, roleName).toUpperCase();
    }

    private String getKey(DefaultRole definition) {
        return getKey(
                definition.getClientId(),
                definition.getName()
        );
    }

    private Map<String, Role> getRoleMap(Realm realm) {
        Map<String, Role> results = new HashMap<>();

        String realmName = realm.getRealm();

        {
            int pageOffset = 0;
            int pageSize = 10;
            int pageCount;
            do {
                List<Role> roles = keycloakRestApi.getRoles(realmName, pageOffset, pageOffset + pageSize);

                for (Role role : roles) {

                    String roleKey = getKey(null, role.getName());

                    results.put(roleKey, role);
                }

                pageCount = roles.size();
                pageOffset += pageCount;
            } while (pageCount >= pageSize);
        }

        {
            int pageOffset = 0;
            int pageSize = 10;
            int pageCount;
            do {
                List<Client> clients = keycloakRestApi.getClients(realmName, pageOffset, pageOffset + pageSize);

                for (Client client : clients) {

                    results.putAll(getRoleMap(client));
                }

                pageCount = clients.size();
                pageOffset += pageCount;
            } while (pageCount >= pageSize);
        }

        return results;
    }

    private Map<String, Role> getRoleMap(Client client) {
        Map<String, Role> results = new HashMap<>();

        int pageOffset = 0;
        int pageSize = 10;
        int pageCount;
        do {
            List<Role> roles = keycloakRestApi.getClientRoles(client.getRealm(), client.getId(), pageOffset, pageOffset + pageSize);

            for (Role role : roles) {

                if (!client.getId().equals(role.getContainerId())) {

                    throw new IllegalStateException("Expecting roles containerId to match client id");
                }

                String roleKey = getKey(role.getContainerId(), role.getName());

                results.put(roleKey, role);
            }

            pageCount = roles.size();
            pageOffset += pageCount;
        } while (pageCount >= pageSize);

        return results;
    }
}
