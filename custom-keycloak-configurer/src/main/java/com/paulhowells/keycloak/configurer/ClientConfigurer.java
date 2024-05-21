package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.ClientDefinition;
import com.paulhowells.keycloak.configurer.model.KeycloakDefinition;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.Client;
import com.paulhowells.keycloak.configurer.rest.client.model.ClientSecret;
import com.paulhowells.keycloak.configurer.rest.client.model.Flow;
import com.paulhowells.keycloak.configurer.rest.client.model.Realm;
import org.slf4j.Logger;

import java.util.*;

import static com.paulhowells.keycloak.configurer.rest.client.model.Client.*;
import static com.paulhowells.keycloak.configurer.rest.client.model.Realm.BROWSER_FLOW_KEY;
import static com.paulhowells.keycloak.configurer.rest.client.model.Realm.DIRECT_GRANT_FLOW_KEY;

public class ClientConfigurer {
    private static final String MANAGED_BY_ATTRIBUTE = "managed-by";
    private static final String MANAGED_BY_ATTRIBUTE_VALUE = "client-configurer";

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;
    private final ClientScopeConfigurer clientScopeConfigurer;

    ClientConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
        this.clientScopeConfigurer = new ClientScopeConfigurer(
                keycloakRestApi,
                logger
        );
    }

    boolean validateDefinitions(
            Map<String, KeycloakDefinition> keycloakDefinitionMap,
            Map<String, Pair<String, ClientDefinition>> definitions
    ) {

        var result = true;

        for (String path:keycloakDefinitionMap.keySet()) {

            KeycloakDefinition keycloakDefinition = keycloakDefinitionMap.get(path);

            for (int index=0;index<keycloakDefinition.getClients().size();++index) {

                ClientDefinition clientDefinition = keycloakDefinition.getClients().get(index);

                var valid = true;

                String realmName = clientDefinition.getRealmName();

                if (realmName == null || realmName.isBlank()) {
                    logger.error("Client Definition {} in {} is missing the realmName", index, path);
                    valid = false;
                }

                String clientId = clientDefinition.getClientId();

                if (clientId == null || clientId.isBlank()) {
                    logger.error("Client Definition {} in {} is missing the clientId", index, path);
                    valid = false;
                }

                String clientKey = getKey(realmName, clientId);

                if (valid) {

                    Pair<String, ClientDefinition> existingClient = definitions.get(clientKey);
                    if (existingClient != null) {
                        logger.error("Duplicate Client Definitions with clientId '{}' in {} and {}", clientId, path, existingClient.first());
                        valid = false;
                    }
                }

                // TODO Validate properties

                if (valid) {
                    definitions.put(clientKey, new Pair<>(path, clientDefinition));
                } else {
                    result = false;
                }
            }
        }

        return result;
    }

    boolean applyClients(
            KeycloakDefinition keycloakDefinition,
            boolean deleteManagedResourcesWhenRemoved
    ) {

        List<ClientDefinition> insertClientDefinitions = new ArrayList<>();
        Map<String, ClientDefinition> updateClientPkMap = new HashMap<>();
        Set<Client> referencedClients = new HashSet<>();
        Map<String, Client> existingClientMap = getResourceMap();

        // Identify if the resource is new or if a resource needs to be updated
        // Keep track of which resource are in use, so we know which ones need to be deleted later
        identifyUpdates(
                existingClientMap,
                keycloakDefinition,
                insertClientDefinitions,
                updateClientPkMap,
                referencedClients
        );

        List<String> referencedClientKeys = getReferencedResourceKeys(referencedClients);
        Map<String, Client> removedClientMap = getRemovedResourceMap(existingClientMap, referencedClientKeys);

        return processUpdates(
                removedClientMap,
                insertClientDefinitions,
                updateClientPkMap,
                deleteManagedResourcesWhenRemoved
        );
    }

    private Map<String, Client> getResourceMap() {
        Map<String, Client> results = new HashMap<>();

        List<Realm> realms = keycloakRestApi.getRealms();

        for(Realm realm:realms) {

            String realmName = realm.getRealm();

            int pageOffset = 0;
            int pageSize = 10;
            int pageCount;
            do {
                List<Client> clients = keycloakRestApi.getClients(realmName, pageOffset, pageOffset + pageSize);

                for (Client client:clients) {

                    String clientKey = getKey(realmName, client.getClientId());

                    results.put(clientKey, client);
                }

                pageCount = clients.size();
                pageOffset += pageCount;
            } while (pageCount >= pageSize);
        }

        return results;
    }

    private void identifyUpdates(
            Map<String, Client> existingResourceMap,
            KeycloakDefinition keycloakDefinition,
            List<ClientDefinition> insertDefinitions,
            Map<String, ClientDefinition> updateDefinitionsPkMap,
            Set<Client> referencedResources
    ) {

        // Identify if the definition is new or if an existing definition needs to be updated
        // Keep track of which definitions are in use, so we know which ones need to be deleted later
        for (ClientDefinition definition: keycloakDefinition.getClients()) {

            String key = getKey(definition.getRealmName(), definition.getClientId());

            Client existingResource = existingResourceMap.get(key);
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
            Map<String, Client> removedResourceMap,
            List<ClientDefinition> insertDefinitions,
            Map<String, ClientDefinition> updateDefinitionMap,
            boolean deleteManagedResourcesWhenRemoved
    ) {
        boolean result = false;

        // Delete the obsolete  Clients
        for (String clientKey:removedResourceMap.keySet()) {

            Client client = removedResourceMap.get(clientKey);

            result = removeResource(
                    client,
                    deleteManagedResourcesWhenRemoved
            ) || result;
        }

        // Insert the new  Clients
        for (ClientDefinition clientDefinition:insertDefinitions) {

            createResource(
                    clientDefinition
            );
            result = true;
        }

        // Update the existing  Clients
        for (ClientDefinition clientDefinition:updateDefinitionMap.values()) {

            result = updateResource(
                    clientDefinition
            ) || result;
        }

        return result;
    }

    static boolean isManaged(Client resource) {

        Map<String, String> attributes = resource.getAttributes();
        String mangedByAttribute = attributes.get(MANAGED_BY_ATTRIBUTE);

        return (MANAGED_BY_ATTRIBUTE_VALUE.equals(mangedByAttribute));
    }

    private boolean removeResource(
            Client client,
            boolean deleteManagedResourcesWhenRemoved
    ) {
        logger.debug("<removeResource");
        boolean result = false;

        // Don't remove an unmanaged client
        if (isManaged(client)) {

            if (deleteManagedResourcesWhenRemoved) {

                logger.warn("Deleting client {} {}", client.getRealm(), client.getClientId());

                keycloakRestApi.deleteClient(client.getRealm(), client.getId());
                result = true;
            } else if(Boolean.TRUE.equals(client.getEnabled())) {

                logger.info("Disabling {} client {}", client.getRealm(), client.getClientId());

                client.setEnabled(Boolean.FALSE);

                keycloakRestApi.updateClient(client.getRealm(), client.getId(), client);
                result = true;
            }
        } else {

            logger.debug("{} client {} not managed by {}", client.getRealm(), client.getClientId(), MANAGED_BY_ATTRIBUTE_VALUE);
        }

        logger.debug(">removeResource");
        return result;
    }

    private void createResource(
            ClientDefinition clientDefinition
    ) {
        logger.debug("<createResource");

        logger.info("Creating {} client {}", clientDefinition.getRealmName(), clientDefinition.getClientId());

        Client client = new Client();

        // Apply changes to resource
        applyDefinition(
                clientDefinition,
                client
        );

        // Tag the resource as managed by the Keycloak Configurer
        client.getAttributes().put(MANAGED_BY_ATTRIBUTE, MANAGED_BY_ATTRIBUTE_VALUE);

        String id = keycloakRestApi.createClient(clientDefinition.getRealmName(), client);

        client = keycloakRestApi.getClient(clientDefinition.getRealmName(), id);

        clientScopeConfigurer.applyScopes(
                client,
                clientDefinition
        );

        logger.debug(">createResource {}", client);
    }

    private boolean updateResource(
            ClientDefinition clientDefinition
    ) {
        logger.debug("<updateResource");
        boolean result = false;

        Client client = keycloakRestApi.getClient(
                clientDefinition.getRealmName(),
                clientDefinition.getId()
        );

        // Don't update an unmanaged client
        if (isManaged(client)) {

            ClientDefinition current = getDefinition(
                    client
            );

            logger.info("Checking {} client {} for updates", clientDefinition.getRealmName(), clientDefinition.getClientId());

            if (isDirty(current, clientDefinition)) {

                logger.info("Updating {} client {}", clientDefinition.getRealmName(), clientDefinition.getClientId());

                // Apply changes to resource
                applyDefinition(
                        clientDefinition,
                        client
                );

                // Tag the resource as managed by the Keycloak Configurer
                client.getAttributes().put(MANAGED_BY_ATTRIBUTE, MANAGED_BY_ATTRIBUTE_VALUE);

                keycloakRestApi.updateClient(
                        clientDefinition.getRealmName(),
                        clientDefinition.getId(),
                        client
                );
                result = true;

                client = keycloakRestApi.getClient(
                        clientDefinition.getRealmName(),
                        clientDefinition.getId()
                );
            } else {

                logger.info("No Change");
            }

            result = clientScopeConfigurer.applyScopes(
                    client,
                    clientDefinition
            ) || result;
        }

        logger.debug(">updateResource {}", result);
        return result;
    }

    List<ClientDefinition> getDefinitions(Realm realm) {
        List<ClientDefinition> results = new ArrayList<>();

        int pageOffset = 0;
        int pageSize = 10;
        int pageCount;
        do {
            List<Client> resources = keycloakRestApi.getClients(realm.getRealm(), pageOffset, pageOffset + pageSize);

            for (Client resource:resources) {

                if (ClientConfigurer.isManaged(resource)) {

                    ClientDefinition definition = getDefinition(resource);
                    results.add(definition);
                }
            }

            pageCount = resources.size();
            pageOffset += pageCount;
        } while (pageCount >= pageSize);

        return results;
    }

    private ClientDefinition getDefinition(
            Client resource
    ) {
        ClientDefinition result = new ClientDefinition();

        result.setRealmName(resource.getRealm());
        result.setClientId(resource.getClientId());

        ClientSecret clientSecret = keycloakRestApi.getClientSecret(resource.getRealm(), resource.getId());
        if (clientSecret != null) {
            result.setClientSecret(clientSecret.getValue());
        }

        result.setEnabled(resource.getEnabled());

        ClientDefinition.GrantTypes grantTypes = result.getGrantTypes();
        if (Boolean.TRUE.equals(resource.getStandardFlowEnabled())) {
            ClientDefinition.GrantTypes.AuthorizationCode authorizationCodeGrant = grantTypes.getAuthorizationCode();
            authorizationCodeGrant.setEnabled(Boolean.TRUE);
            authorizationCodeGrant.setPostLoginRedirectUris(resource.getRedirectUris());
            authorizationCodeGrant.getWebOrigins().addAll(resource.getWebOrigins());

            String postLogoutRedirectUris = resource.getAttributes().get(POST_LOGOUT_REDIRECT_URIS_ATTRIBUTE);
            if (postLogoutRedirectUris!=null&&!postLogoutRedirectUris.isBlank()) {

                String[] postLogoutRedirectUriArray = postLogoutRedirectUris.split("##");
                List<String> postLogoutRedirectUriList = new ArrayList<>(Arrays.asList(postLogoutRedirectUriArray));
                authorizationCodeGrant.setPostLogoutRedirectUris(postLogoutRedirectUriList);
            }

            String authenticationFlowId = resource.getAuthenticationFlowBindingOverrides().get(BROWSER_FLOW_KEY);
            if (authenticationFlowId!=null) {

                Flow authenticationFlow = keycloakRestApi.getTopLevelFlow(resource.getRealm(), authenticationFlowId);
                authorizationCodeGrant.setFlowOverride(authenticationFlow.getAlias());
            }
        }

        if (Boolean.TRUE.equals(resource.getDirectAccessGrantsEnabled())) {
            ClientDefinition.GrantTypes.Password passwordGrant = grantTypes.getPassword();
            passwordGrant.setEnabled(Boolean.TRUE);

            String authenticationFlowId = resource.getAuthenticationFlowBindingOverrides().get(DIRECT_GRANT_FLOW_KEY);
            if (authenticationFlowId!=null) {

                Flow authenticationFlow = keycloakRestApi.getTopLevelFlow(resource.getRealm(), authenticationFlowId);
                passwordGrant.setFlowOverride(authenticationFlow.getAlias());
            }
        }

        if (Boolean.TRUE.equals(resource.getServiceAccountsEnabled())) {
            ClientDefinition.GrantTypes.ClientCredentials clientCredentialsGrant = grantTypes.getClientCredentials();
            clientCredentialsGrant.setEnabled(Boolean.TRUE);
        }

        result.setScopes(clientScopeConfigurer.getDefinitions(resource));

        ClientDefinition.AdvancedSettings advancedSettings = result.getAdvancedSettings();

        String accessTokenLifespanSeconds = resource.getAttributes().get(ACCESS_TOKEN_LIFESPAN_ATTRIBUTE);
        if (accessTokenLifespanSeconds != null && !accessTokenLifespanSeconds.isBlank()) {

            advancedSettings.setAccessTokenLifespanSeconds(Long.valueOf(accessTokenLifespanSeconds));
        }

        String clientSessionIdleTimeoutSeconds = resource.getAttributes().get(CLIENT_SESSION_IDLE_TIMEOUT_SECONDS_ATTRIBUTE);
        if (clientSessionIdleTimeoutSeconds != null && !clientSessionIdleTimeoutSeconds.isBlank()) {

            advancedSettings.setClientSessionIdleTimeoutSeconds(Long.valueOf(clientSessionIdleTimeoutSeconds));
        }

        String clientSessionMaxLifespanSeconds = resource.getAttributes().get(CLIENT_SESSION_MAX_LIFESPAN_SECONDS_ATTRIBUTE);
        if (clientSessionMaxLifespanSeconds != null && !clientSessionMaxLifespanSeconds.isBlank()) {

            advancedSettings.setClientSessionMaxLifespanSeconds(Long.valueOf(clientSessionMaxLifespanSeconds));
        }

        String clientOfflineSessionIdleTimeoutSeconds = resource.getAttributes().get(CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT_SECONDS_ATTRIBUTE);
        if (clientOfflineSessionIdleTimeoutSeconds != null && !clientOfflineSessionIdleTimeoutSeconds.isBlank()) {

            advancedSettings.setClientOfflineSessionIdleTimeoutSeconds(Long.valueOf(clientOfflineSessionIdleTimeoutSeconds));
        }

        String clientOfflineSessionMaxLifespanSeconds = resource.getAttributes().get(CLIENT_OFFLINE_SESSION_MAX_LIFESPAN_SECONDS_ATTRIBUTE);
        if (clientOfflineSessionMaxLifespanSeconds != null && !clientOfflineSessionMaxLifespanSeconds.isBlank()) {

            advancedSettings.setClientOfflineSessionMaxLifespanSeconds(Long.valueOf(clientOfflineSessionMaxLifespanSeconds));
        }

        return result;
    }

    private void applyDefinition(
            ClientDefinition definition,
            Client resource
    ) {
        resource.setClientId(definition.getClientId());

        resource.setSecret(definition.getClientSecret());
        resource.setPublicClient(definition.getClientSecret() == null);

        resource.setEnabled(definition.getEnabled());

        // Reset the enabled flows
        resource.setServiceAccountsEnabled(Boolean.FALSE);
        resource.setStandardFlowEnabled(Boolean.FALSE);
        resource.setDirectAccessGrantsEnabled(Boolean.FALSE);
        resource.getAttributes().remove(DEVICE_AUTHORIZATION_GRANT_ATTRIBUTE);
        resource.getAttributes().remove(OIDC_CIBA_GRANT_ATTRIBUTE);
        resource.getWebOrigins().clear();

        List<Flow> authenticationFlows = keycloakRestApi.getTopLevelFlows(definition.getRealmName());

        ClientDefinition.GrantTypes grantTypes = definition.getGrantTypes();

        ClientDefinition.GrantTypes.AuthorizationCode authorizationCodeGrant = grantTypes.getAuthorizationCode();
        if (authorizationCodeGrant !=null && Boolean.TRUE.equals(authorizationCodeGrant.getEnabled())) {
            resource.setStandardFlowEnabled(Boolean.TRUE);
            resource.getRedirectUris().addAll(authorizationCodeGrant.getPostLoginRedirectUris());

            List<String> webOrigins = authorizationCodeGrant.getWebOrigins();
            resource.getWebOrigins().addAll(webOrigins.stream().map(String::trim).toList());

            List<String> postLogoutRedirectUriList = authorizationCodeGrant.getPostLogoutRedirectUris();
            // Keycloak uses ## to delimit the URLs
            String postLogoutRedirectUris = String.join("##", postLogoutRedirectUriList);
            resource.getAttributes().put(POST_LOGOUT_REDIRECT_URIS_ATTRIBUTE, postLogoutRedirectUris);

            String flowOverride = authorizationCodeGrant.getFlowOverride();

            if (flowOverride != null) {

                // locate the specified flow in keycloak:
                Flow authenticationFlow = authenticationFlows.stream().filter(it -> flowOverride.equals(it.getAlias())).toList().get(0);

                if (authenticationFlow != null) {

                    // add the flow as an override:
                    resource.getAuthenticationFlowBindingOverrides().put(BROWSER_FLOW_KEY, authenticationFlow.getId());
                } else {

                    // if the specified flow cannot be found, the service client cannot be configured as intended:
                    throw new RuntimeException(String.format("Custom password flow not available. alias=%s", flowOverride));
                }
            }
        }

        ClientDefinition.GrantTypes.Password passwordGrant = grantTypes.getPassword();
        if (passwordGrant != null && Boolean.TRUE.equals(passwordGrant.getEnabled())) {
            resource.setDirectAccessGrantsEnabled(Boolean.TRUE);

            String flowOverride = passwordGrant.getFlowOverride();

            if (flowOverride != null) {

                // locate the specified flow in keycloak:
                Flow authenticationFlow = null;
                for (Flow flow:authenticationFlows) {
                    logger.debug(flow.getAlias());
                    if (flowOverride.equals(flow.getAlias())) {
                        authenticationFlow = flow;
                        break;
                    }
                }

                if (authenticationFlow != null) {

                    // add the flow as an override:
                    resource.getAuthenticationFlowBindingOverrides().put(DIRECT_GRANT_FLOW_KEY, authenticationFlow.getId());
                } else {

                    // if the specified flow cannot be found, the service client cannot be configured as intended:
                    throw new RuntimeException(String.format("Custom password flow not available. alias=%s", flowOverride));
                }
            }
        }

        ClientDefinition.GrantTypes.ClientCredentials clientCredentialsGrant = grantTypes.getClientCredentials();
        if (clientCredentialsGrant !=null && Boolean.TRUE.equals(clientCredentialsGrant.getEnabled())) {
            resource.setServiceAccountsEnabled(Boolean.TRUE);
        }

        // Reset the access token settings
        resource.getAttributes().remove(ACCESS_TOKEN_LIFESPAN_ATTRIBUTE);
        resource.getAttributes().remove(CLIENT_SESSION_IDLE_TIMEOUT_SECONDS_ATTRIBUTE);
        resource.getAttributes().remove(CLIENT_SESSION_MAX_LIFESPAN_SECONDS_ATTRIBUTE);
        resource.getAttributes().remove(CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT_SECONDS_ATTRIBUTE);
        resource.getAttributes().remove(CLIENT_OFFLINE_SESSION_MAX_LIFESPAN_SECONDS_ATTRIBUTE);

        ClientDefinition.AdvancedSettings advancedSettings = definition.getAdvancedSettings();
        if (advancedSettings != null) {

            Long accessTokenLifespanSeconds = advancedSettings.getAccessTokenLifespanSeconds();
            if (accessTokenLifespanSeconds != null) {

                resource.getAttributes().put(ACCESS_TOKEN_LIFESPAN_ATTRIBUTE, accessTokenLifespanSeconds.toString());
            }

            Long clientSessionIdleTimeoutSeconds = advancedSettings.getClientSessionIdleTimeoutSeconds();
            if (clientSessionIdleTimeoutSeconds != null) {

                resource.getAttributes().put(CLIENT_SESSION_IDLE_TIMEOUT_SECONDS_ATTRIBUTE, clientSessionIdleTimeoutSeconds.toString());
            }

            Long clientSessionMaxLifespanSeconds = advancedSettings.getClientSessionMaxLifespanSeconds();
            if (clientSessionMaxLifespanSeconds != null) {

                resource.getAttributes().put(CLIENT_SESSION_MAX_LIFESPAN_SECONDS_ATTRIBUTE, clientSessionMaxLifespanSeconds.toString());
            }

            Long clientOfflineSessionIdleTimeoutSeconds = advancedSettings.getClientOfflineSessionIdleTimeoutSeconds();
            if (clientOfflineSessionIdleTimeoutSeconds != null) {

                resource.getAttributes().put(CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT_SECONDS_ATTRIBUTE, clientOfflineSessionIdleTimeoutSeconds.toString());
            }

            Long clientOfflineSessionMaxLifespanSeconds = advancedSettings.getClientOfflineSessionMaxLifespanSeconds();
            if (clientOfflineSessionMaxLifespanSeconds != null) {

                resource.getAttributes().put(CLIENT_OFFLINE_SESSION_MAX_LIFESPAN_SECONDS_ATTRIBUTE, clientOfflineSessionMaxLifespanSeconds.toString());
            }
        }
    }

    private boolean isDirty(
            ClientDefinition current,
            ClientDefinition updated
    ) {
        return !current.isUnchanged(updated, null, logger);
    }

    private static Map<String, Client> getRemovedResourceMap(Map<String, Client> existingResourceMap, List<String> referencedKeys) {
        Map<String, Client> result = new HashMap<>();

        for (String key: existingResourceMap.keySet()) {

            // If the resource is not referenced
            if (!referencedKeys.contains(key)) {

                result.put(key, existingResourceMap.get(key));
            }
        }

        return result;
    }

    private static List<String> getReferencedResourceKeys(Set<Client> referencedResources) {
        return referencedResources.stream().map(it -> getKey(it.getRealm(), it.getClientId())).toList();
    }

    private static String getKey(
            String realmName,
            String clientId
    ) {
        return String.format("%s:%s", realmName, clientId).toUpperCase();
    }
}
