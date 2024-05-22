package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.KeycloakDefinition;
import com.paulhowells.keycloak.configurer.model.RealmDefinition;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.*;
import org.slf4j.Logger;

import java.util.*;

public class RealmConfigurer {
    private static final String MANAGED_BY_ATTRIBUTE = "managed-by";
    static final String MANAGED_BY_ATTRIBUTE_VALUE = "realm-configurer";

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    private final GeneralSettingsConfigurer generalSettingsConfigurer;

    private final LoginSettingsConfigurer loginSettingsConfigurer;

    private final EmailSettingsConfigurer emailSettingsConfigurer;

    private final EventSettingsConfigurer eventSettingsConfigurer;

    private final SessionSettingsConfigurer sessionSettingsConfigurer;

    private final TokenSettingsConfigurer tokenSettingsConfigurer;

    private final UserProfileConfigurer userProfileConfigurer;

    private final DefaultRoleConfigurer defaultRoleConfigurer;

    private final ScopeConfigurer scopeConfigurer;

    private final RealmRoleConfigurer realmRoleConfigurer;

    private final PasswordPolicyConfigurer passwordPolicyConfigurer;

    private final GoolgeIdpConfigurer googleIdpConfigurer;

    RealmConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
        this.generalSettingsConfigurer = new GeneralSettingsConfigurer(
                keycloakRestApi,
                logger
        );
        this.loginSettingsConfigurer = new LoginSettingsConfigurer(
                keycloakRestApi,
                logger
        );
        this.emailSettingsConfigurer = new EmailSettingsConfigurer(
                keycloakRestApi,
                logger
        );
        this.eventSettingsConfigurer = new EventSettingsConfigurer(
                keycloakRestApi,
                logger
        );
        this.sessionSettingsConfigurer = new SessionSettingsConfigurer(
                keycloakRestApi,
                logger
        );
        this.tokenSettingsConfigurer = new TokenSettingsConfigurer(
                keycloakRestApi,
                logger
        );
        this.userProfileConfigurer = new UserProfileConfigurer(
                keycloakRestApi,
                logger
        );
        this.defaultRoleConfigurer = new DefaultRoleConfigurer(
                keycloakRestApi,
                logger
        );
        this.scopeConfigurer = new ScopeConfigurer(
                keycloakRestApi,
                logger
        );
        this.realmRoleConfigurer = new RealmRoleConfigurer(
                keycloakRestApi,
                logger
        );
        this.passwordPolicyConfigurer = new PasswordPolicyConfigurer(
                keycloakRestApi,
                logger
        );
        this.googleIdpConfigurer = new GoolgeIdpConfigurer(
                keycloakRestApi,
                logger
        );
    }

    public boolean validateDefinitions(
            Map<String, KeycloakDefinition> keycloakDefinitionMap,
            Map<String, Pair<String, RealmDefinition>> definitions
    ) {
        boolean result = true;

        for (String path:keycloakDefinitionMap.keySet()) {

            KeycloakDefinition keycloakDefinition = keycloakDefinitionMap.get(path);

            for (int index=0;index<keycloakDefinition.getRealms().size();++index) {

                RealmDefinition realmDefinition = keycloakDefinition.getRealms().get(index);

                boolean valid = true;

                String realmName = realmDefinition.getRealmName();

                if (realmName == null || realmName.isBlank()) {
                    logger.error("Realm Definition {} in {} is missing the realmName", index, path);
                    valid = false;
                }

                String realmKey = getKey(realmName);

                if (valid) {

                    Pair<String, RealmDefinition> existingRealm = definitions.get(realmKey);
                    if (existingRealm != null) {
                        logger.error("Duplicate Realm Definitions with name '{}' in {} and {}", realmName, path, existingRealm.first());
                        valid = false;
                    }
                }

                if (valid) {
                    definitions.put(realmKey, new Pair<>(path, realmDefinition));
                } else {
                    result = false;
                }
            }
        }

        return result;
    }

    boolean applyRealms(
            KeycloakDefinition keycloakDefinition,
            boolean deleteManagedResourcesWhenRemoved
    ) {

        List<RealmDefinition> insertRealmDefinitions = new ArrayList<>();
        Map<String, RealmDefinition> updateRealmPkMap = new HashMap<>();
        Set<Realm> referencedRealms = new HashSet<>();
        Map<String, Realm> existingRealmMap = getResourceMap();

        // Identify if the resource is new or if a resource needs to be updated
        // Keep track of which resource are in use, so we know which ones need to be deleted later
        identifyUpdates(
                existingRealmMap,
                keycloakDefinition,
                insertRealmDefinitions,
                updateRealmPkMap,
                referencedRealms
        );

        List<String> referencedRealmKeys = getReferencedResourceKeys(referencedRealms);
        Map<String, Realm> removedRealmMap = getRemovedResourceMap(existingRealmMap, referencedRealmKeys);

        return processUpdates(
                removedRealmMap,
                insertRealmDefinitions,
                updateRealmPkMap,
                deleteManagedResourcesWhenRemoved
        );
    }

    private Map<String, Realm> getResourceMap() {
        Map<String, Realm> results = new HashMap<>();

        List<Realm> realms = keycloakRestApi.getRealms();

        for (Realm realm:realms) {

            String realmKey = getKey(realm.getRealm());

            results.put(realmKey, realm);
        }

        return results;
    }

    private void identifyUpdates(
            Map<String, Realm> existingResourceMap,
            KeycloakDefinition keycloakDefinition,
            List<RealmDefinition> insertDefinitions,
            Map<String, RealmDefinition> updateDefinitionsPkMap,
            Set<Realm> referencedResources
    ) {

        // Identify if the definition is new or if an existing definition needs to be updated
        // Keep track of which definitions are in use, so we know which ones need to be deleted later
        for (RealmDefinition definition: keycloakDefinition.getRealms()) {

            String key = getKey(definition.getRealmName());

            Realm existingResource = existingResourceMap.get(key);
            if (existingResource == null) {

                insertDefinitions.add(definition);
            } else {

                updateDefinitionsPkMap.put(key, definition);
                referencedResources.add(existingResource);
            }
        }
    }

    private boolean processUpdates(
            Map<String, Realm> removedResourceMap,
            List<RealmDefinition> insertDefinitions,
            Map<String, RealmDefinition> updateDefinitionsMap,
            boolean deleteManagedResourcesWhenRemoved
    ) {
        boolean result = false;

        // Delete the obsolete Realms
        for (String realmKey:removedResourceMap.keySet()) {

            Realm realm = removedResourceMap.get(realmKey);

            result = removeResource(
                    realm,
                    deleteManagedResourcesWhenRemoved
            ) || result;
        }

        // Insert the new Realms
        for (RealmDefinition realmDefinition:insertDefinitions) {

            createResource(
                    realmDefinition
            );
            result = true;
        }

        // Update the existing Realms
        for (RealmDefinition realmDefinition:updateDefinitionsMap.values()) {

            result = updateResource(
                    realmDefinition,
                    deleteManagedResourcesWhenRemoved
            ) || result;
        }

        return result;
    }

    static boolean isManaged(Realm resource) {

        Map<String, String> attributes = resource.getAttributes();
        String mangedByAttribute = attributes.get(MANAGED_BY_ATTRIBUTE);

        return (MANAGED_BY_ATTRIBUTE_VALUE.equals(mangedByAttribute));
    }

    private boolean removeResource(
            Realm resource,
            boolean deleteManagedResourcesWhenRemoved
    ) {
        logger.debug("<removeRealm");
        boolean result = false;

        if (isManaged(resource)) {

            if (deleteManagedResourcesWhenRemoved) {

                logger.warn("Deleting realm {}", resource.getRealm());

                keycloakRestApi.deleteRealm(resource.getRealm());
                result = true;

            } else if (Boolean.TRUE.equals(resource.getEnabled())) {

                logger.info("Disabling realm {}", resource.getRealm());

                resource.setEnabled(Boolean.FALSE);

                keycloakRestApi.updateRealm(resource.getRealm(), resource);
                result = true;
            }
        } else {

            logger.debug("Realm {} not managed by {}", resource.getRealm(), MANAGED_BY_ATTRIBUTE_VALUE);
        }

        logger.debug(">removeRealm");
        return result;
    }

    private void createResource(
            RealmDefinition realmDefinition
    ) {
        logger.debug("<createResource");

        logger.info("Creating realm {}", realmDefinition.getRealmName());

        Realm realm = new Realm();

        applyDefinition(
                realmDefinition,
                realm
        );

        // Tag the resource as managed by the Keycloak Configurer
        realm.getAttributes().put(MANAGED_BY_ATTRIBUTE, MANAGED_BY_ATTRIBUTE_VALUE);

        keycloakRestApi.createRealm(realm);

        realm = keycloakRestApi.getRealmByName(realmDefinition.getRealmName());

        // Do the events first so we can see the events in the logs ASAP
        eventSettingsConfigurer.updateEventSettings(
                realm,
                realmDefinition.getEventSettings()
        );

        generalSettingsConfigurer.updateGeneralSettings(
                realm,
                realmDefinition.getGeneralSettings()
        );

        loginSettingsConfigurer.updateLoginSettings(
                realm,
                realmDefinition.getLoginSettings()
        );

        emailSettingsConfigurer.updateEmailSettings(
                realm,
                realmDefinition.getEmailSettings()
        );

        sessionSettingsConfigurer.updateSessionSettings(
                realm,
                realmDefinition.getSessionSettings()
        );

        tokenSettingsConfigurer.updateTokenSettings(
                realm,
                realmDefinition.getTokenSettings()
        );

        userProfileConfigurer.updateUserProfile(
                realm,
                realmDefinition.getUserProfile()
        );

        defaultRoleConfigurer.applyRoles(
                realm,
                realmDefinition
        );

        scopeConfigurer.applyScopes(
                realm,
                realmDefinition
        );

        realmRoleConfigurer.applyRoles(
                realm,
                realmDefinition
        );

        passwordPolicyConfigurer.updatePasswordPolicy(
                realm,
                realmDefinition.getPasswordPolicy()
        );
        
        googleIdpConfigurer.processUpdate(
                realm,
                realmDefinition.getGoogleIdentityProvider(),
                false
        );

        logger.debug(">createResource");
    }

    private boolean updateResource(
            RealmDefinition realmDefinition,
            boolean deleteManagedResourcesWhenRemoved
    ) {
        logger.debug("<updateRealm");
        boolean result = false;

        Realm realm = keycloakRestApi.getRealmByName(realmDefinition.getRealmName());

        RealmDefinition existingRealmDefinition = getDefinition(
                realm
        );

        logger.info("Checking realm {} for updates", realmDefinition.getRealmName());

        if (isDirty(existingRealmDefinition, realmDefinition)) {

            logger.info("Updating realm {}", realmDefinition.getRealmName());

            // Apply changes to realm
            applyDefinition(
                    realmDefinition,
                    realm
            );

            keycloakRestApi.updateRealm(realmDefinition.getRealmName(), realm);
            result = true;

            realm = keycloakRestApi.getRealmByName(realmDefinition.getRealmName());
        } else {

            logger.info("No Change");
        }

        // Do the events first so we can see the events in the logs ASAP
        result = eventSettingsConfigurer.updateEventSettings(
                realm,
                realmDefinition.getEventSettings()
        ) || result;

        result = generalSettingsConfigurer.updateGeneralSettings(
                realm,
                realmDefinition.getGeneralSettings()
        ) || result;

        result = loginSettingsConfigurer.updateLoginSettings(
                realm,
                realmDefinition.getLoginSettings()
        ) || result;

        result = emailSettingsConfigurer.updateEmailSettings(
                realm,
                realmDefinition.getEmailSettings()
        ) || result;

        result = sessionSettingsConfigurer.updateSessionSettings(
                realm,
                realmDefinition.getSessionSettings()
        ) || result;

        result = tokenSettingsConfigurer.updateTokenSettings(
                realm,
                realmDefinition.getTokenSettings()
        ) || result;

        result = userProfileConfigurer.updateUserProfile(
                realm,
                realmDefinition.getUserProfile()
        ) || result;

        result = defaultRoleConfigurer.applyRoles(
                realm,
                realmDefinition
        ) || result;

        result = scopeConfigurer.applyScopes(
                realm,
                realmDefinition
        ) || result;

        result = realmRoleConfigurer.applyRoles(
                realm,
                realmDefinition
        ) || result;

        result = passwordPolicyConfigurer.updatePasswordPolicy(
                realm,
                realmDefinition.getPasswordPolicy()
        ) || result;
        
        result = googleIdpConfigurer.processUpdate(
                realm,
                realmDefinition.getGoogleIdentityProvider(),
                deleteManagedResourcesWhenRemoved
        ) || result;

        logger.debug(">updateRealm {}", result);
        return result;
    }

    RealmDefinition getDefinition(
            Realm realm
    ) {
        RealmDefinition result = new RealmDefinition();

        result.setRealmName(realm.getRealm());
        result.setDefaultBrowserFlowAlias(realm.getBrowserFlow());
        result.setDefaultRegistrationFlowAlias(realm.getRegistrationFlow());
        result.setDefaultDirectGrantFlowAlias(realm.getDirectGrantFlow());
        result.setDefaultResetCredentialsFlowAlias(realm.getResetCredentialsFlow());
        result.setDefaultClientAuthenticationFlowAlias(realm.getClientAuthenticationFlow());
        result.setDefaultFirstBrokerLoginFlowAlias(realm.getFirstBrokerLoginFlowAlias());
        result.setEnabled(realm.getEnabled());

        result.setGeneralSettings(generalSettingsConfigurer.getDefinition(realm));
        result.setLoginSettings(loginSettingsConfigurer.getDefinition(realm));
        result.setEmailSettings(emailSettingsConfigurer.getDefinition(realm));
        result.setEventSettings(eventSettingsConfigurer.getDefinition(realm));
        result.setSessionSettings(sessionSettingsConfigurer.getDefinition(realm));
        result.setTokenSettings(tokenSettingsConfigurer.getDefinition(realm));
        result.setUserProfile(userProfileConfigurer.getDefinition(realm));
        result.setDefaultRoles(defaultRoleConfigurer.getDefinitions(realm));
        result.getScopes().putAll(scopeConfigurer.getDefinitions(realm));
        result.getRoles().putAll(realmRoleConfigurer.getDefinitions(realm));
        result.setPasswordPolicy(passwordPolicyConfigurer.getDefinition(realm));
        result.setGoogleIdentityProvider(googleIdpConfigurer.getDefinition(realm));

        return result;
    }

    private void applyDefinition(
            RealmDefinition realmDefinition,
            Realm realm
    ) {

        realm.setRealm(realmDefinition.getRealmName());
        realm.setBrowserFlow(realmDefinition.getDefaultBrowserFlowAlias());
        realm.setRegistrationFlow(realmDefinition.getDefaultRegistrationFlowAlias());
        realm.setDirectGrantFlow(realmDefinition.getDefaultDirectGrantFlowAlias());
        realm.setResetCredentialsFlow(realmDefinition.getDefaultResetCredentialsFlowAlias());
        realm.setClientAuthenticationFlow(realmDefinition.getDefaultClientAuthenticationFlowAlias());
        realm.setFirstBrokerLoginFlowAlias(realmDefinition.getDefaultFirstBrokerLoginFlowAlias());
        realm.setEnabled(realmDefinition.getEnabled());
    }

    private boolean isDirty(
            RealmDefinition current,
            RealmDefinition updated) {
        return !current.isUnchanged(updated, null, logger);
    }

    private static Map<String, Realm> getRemovedResourceMap(Map<String, Realm> existingResourceMap, List<String> referencedKeys) {
        Map<String, Realm> result = new HashMap<>();

        for (String key: existingResourceMap.keySet()) {

            // If the resource is not referenced
            if (!referencedKeys.contains(key)) {

                result.put(key, existingResourceMap.get(key));
            }
        }

        return result;
    }

    private static List<String> getReferencedResourceKeys(Set<Realm> referencedResources) {
        return referencedResources.stream().map(it -> getKey(it.getRealm())).toList();
    }

    private static String getKey(
            String realmName
    ) {
        return String.format("%s", realmName).toUpperCase();
    }
}
