package com.paulhowells.keycloak.configurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.paulhowells.keycloak.configurer.model.*;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.paulhowells.keycloak.configurer.rest.client.model.IdentityProvider.GOOGLE_IDP_ALIAS;

/**
 * Import the Keycloak Definitions found in the provided file system directory.
 * <p>
 * The import will insert, update, and delete the current Keycloak configuration as required to obtain the configuration
 * state described by the Keycloak Definitions.
 * <p>
 * The import process will validate that the Keycloak Definitions describe a valid configuration state before proceeding
 * with the update.
 */
public class KeycloakConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakConfigurer.class);

    private static final String DefaultBrowserFlowBindingReference = "Default Browser Flow Binding";
    private static final String DefaultRegistrationFlowBindingReference = "Default Registration Flow Binding";
    private static final String DefaultDirectGrantFlowBindingReference = "Default Direct Grant Flow Binding";
    private static final String DefaultResetCredentialsFlowBindingReference = "Default Reset Credentials Flow Binding";
    private static final String DefaultClientAuthenticationFlowBindingReference = "Default Client Authentication Flow Binding";

    private static final List<String> DefaultFlowBindingReferences;
    public static final String INVALID = "INVALID";

    static {
        DefaultFlowBindingReferences = new ArrayList<>();
        DefaultFlowBindingReferences.add(DefaultBrowserFlowBindingReference);
        DefaultFlowBindingReferences.add(DefaultRegistrationFlowBindingReference);
        DefaultFlowBindingReferences.add(DefaultDirectGrantFlowBindingReference);
        DefaultFlowBindingReferences.add(DefaultResetCredentialsFlowBindingReference);
        DefaultFlowBindingReferences.add(DefaultClientAuthenticationFlowBindingReference);
    }

    private static final String DefaultBrowserFlowAlias = "browser";
    private static final String DefaultRegistrationFlowAlias = "registration";
    private static final String DefaultDirectGrantFlowAlias = "direct grant";
    private static final String DefaultResetCredentialsFlowAlias = "reset credentials";
    private static final String DefaultClientAuthenticationFlowAlias = "clients";

    private static final List<String> DefaultFlowAliases;

    static {
        DefaultFlowAliases = new ArrayList<>();
        DefaultFlowAliases.add(DefaultBrowserFlowAlias);
        DefaultFlowAliases.add(DefaultRegistrationFlowAlias);
        DefaultFlowAliases.add(DefaultDirectGrantFlowAlias);
        DefaultFlowAliases.add(DefaultResetCredentialsFlowAlias);
        DefaultFlowAliases.add(DefaultClientAuthenticationFlowAlias);
    }

    public static final String KEYCLOAK_CONFIG_DIRECTORY_ARG = "--config=";
    public static final String KEYCLOAK_CONFIG_URL_ARG = "--keycloak-url=";
    public static final String KEYCLOAK_CONFIG_DIRECTORY_ENV_VARIABLE = "KEYCLOAK_CONFIG_DIRECTORY";
    public static final String KEYCLOAK_URL_ENV_VARIABLE = "KEYCLOAK_URL";
    public static final String MASTER_REALM_ADMIN_USERNAME_ENV_VARIABLE = "MASTER_REALM_ADMIN_USERNAME";
    public static final String MASTER_REALM_ADMIN_PASSWORD_ENV_VARIABLE = "MASTER_REALM_ADMIN_PASSWORD";
    public static final String MASTER_REALM_NAME = "master";

    public static void main(String[] args) throws IOException {
        logger.debug("<main");

        String configDirectory = null;
        String keycloakUrl = null;
        for (String arg:args) {
            if (arg.startsWith(KEYCLOAK_CONFIG_DIRECTORY_ARG)) {

                configDirectory = arg.substring(KEYCLOAK_CONFIG_DIRECTORY_ARG.length());
                logger.info("{} '{}' Found config directory", KEYCLOAK_CONFIG_DIRECTORY_ARG, configDirectory);
            }
            if (arg.startsWith(KEYCLOAK_CONFIG_URL_ARG)) {

                keycloakUrl = arg.substring(KEYCLOAK_CONFIG_URL_ARG.length());
                logger.info("{} '{}' Found keycloak URL", KEYCLOAK_CONFIG_URL_ARG, keycloakUrl);
            }
        }

        new KeycloakConfigurer().run(configDirectory, keycloakUrl);

        logger.debug(">main");
    }

    private RealmConfigurer realmConfigurer;
    private AuthenticationFlowConfigurer authenticationFlowConfigurer;
    private ClientConfigurer clientConfigurer;

    private KeycloakConfigurer() {
        logger.debug("<KeycloakConfigurer");

        logger.debug(">KeycloakConfigurer");
    }

    public static KeycloakDefinition getDefinition(
            KeycloakRestApi keycloakRestApi
    ) {
        KeycloakDefinition result = new KeycloakDefinition();

        RealmConfigurer realmConfigurer = new RealmConfigurer(keycloakRestApi, logger);
        ClientConfigurer clientConfigurer = new ClientConfigurer(keycloakRestApi, logger);
        AuthenticationFlowConfigurer authenticationFlowConfigurer = new AuthenticationFlowConfigurer(keycloakRestApi, logger);

        for(Realm realm:keycloakRestApi.getRealms()) {

            RealmDefinition realmDefinition = realmConfigurer.getDefinition(realm);

            List<AuthenticationFlowDefinition> authenticationFlows = authenticationFlowConfigurer.getDefinitions(realm);
            result.getAuthenticationFlows().addAll(authenticationFlows);

            List<ClientDefinition> clients = clientConfigurer.getDefinitions(realm);
            result.getClients().addAll(clients);

            result.getRealms().add(realmDefinition);
        }

        return result;
    }

    private void run(String configDirectory, String keycloakUrl) throws IOException {
        logger.debug("<run");

        boolean configValid = true;
        if (configDirectory == null) {
            configDirectory = System.getenv(KEYCLOAK_CONFIG_DIRECTORY_ENV_VARIABLE);
            if (configDirectory == null) {
                configValid = false;
                logger.warn("No config directory provided. ({})", KEYCLOAK_CONFIG_DIRECTORY_ENV_VARIABLE);
            } else {
                logger.info("{} = '{}' Found config directory", KEYCLOAK_CONFIG_DIRECTORY_ENV_VARIABLE, configDirectory);
            }
        }
        if (keycloakUrl==null || keycloakUrl.isBlank()) {
            keycloakUrl = System.getenv(KEYCLOAK_URL_ENV_VARIABLE);
            if (keycloakUrl == null) {
                configValid = false;
                logger.warn("No keycloak URL provided.. ({})", KEYCLOAK_URL_ENV_VARIABLE);
            } else {
                logger.info("{} = '{}' Found Keycloak URL", KEYCLOAK_URL_ENV_VARIABLE, keycloakUrl);
            }
        }
        String realmAdminUsername = System.getenv(MASTER_REALM_ADMIN_USERNAME_ENV_VARIABLE);
        if (realmAdminUsername == null) {
            configValid = false;
            logger.warn("No realm admin username provided. ({})", MASTER_REALM_ADMIN_USERNAME_ENV_VARIABLE);
        } else {
            logger.info("{} = '{}' Found Realm Admin username", MASTER_REALM_ADMIN_USERNAME_ENV_VARIABLE, realmAdminUsername);
        }
        String realmAdminPassword = System.getenv(MASTER_REALM_ADMIN_PASSWORD_ENV_VARIABLE);
        if (realmAdminPassword == null) {
            configValid = false;
            logger.warn("No realm admin password provided. ({})", MASTER_REALM_ADMIN_PASSWORD_ENV_VARIABLE);
        } else {
            logger.info("{} = ******** Found Realm Admin password", MASTER_REALM_ADMIN_PASSWORD_ENV_VARIABLE);
        }

        if (configValid) {

            try {
                logger.info("********************************************************************************");
                logger.info("********************************************************************************");
                logger.info("Starting Keycloak Definition Import");

                try (KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                        keycloakUrl,
                        MASTER_REALM_NAME,
                        realmAdminUsername,
                        realmAdminPassword
                )) {
                    this.realmConfigurer = new RealmConfigurer(
                            keycloakRestApi,
                            logger
                    );
                    this.authenticationFlowConfigurer = new AuthenticationFlowConfigurer(
                            keycloakRestApi,
                            logger
                    );
                    this.clientConfigurer = new ClientConfigurer(
                            keycloakRestApi,
                            logger
                    );

                    final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

                    Map<String, KeycloakDefinition> keycloakDefinitionMap = loadKeycloakDefinitions(objectMapper, configDirectory);
                    logger.info("Found {} Keycloak definitions", keycloakDefinitionMap.size());
    
                    KeycloakDefinition keycloakDefinition = validateKeycloakDefinitions(keycloakRestApi, keycloakDefinitionMap);
    
                    boolean result = false;
                    if (keycloakDefinition != null) {
                        // The Keycloak Definitions are valid
    
                        result = applyKeycloakDefinition(
                                keycloakDefinition
                        );
                    }

                    if (result) {
                        logger.info("Completed Keycloak Definition Import");
                    } else {
                        logger.warn("Aborted Keycloak Definition Import");
                    }

                }
            } catch (Exception e) {
                logger.error("Keycloak Definition Import Failed", e);
                throw e;
            } finally {
                logger.info("********************************************************************************");
                logger.info("********************************************************************************");
            }
        }

        logger.debug(">run");
    }

    private Map<String, KeycloakDefinition> loadKeycloakDefinitions(
            ObjectMapper objectMapper,
            String configDirectory
    ) throws IOException {

        Map<String, KeycloakDefinition> results = new HashMap<>();

        logger.info("Loading Keycloak definitions from directory '{}'", configDirectory);

        Path configDirectoryPath = Paths.get(configDirectory);

        Files.walkFileTree(
                configDirectoryPath,
                new SimpleFileVisitor<>() {
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) throws IOException {

                        String fileName = path.toString();
                        if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {

                            logger.info("Found Keycloak definition file '{}'", path);

                            KeycloakDefinition keycloakDefinition;
                            try {
                                keycloakDefinition = objectMapper.readValue(path.toFile(), KeycloakDefinition.class);
                            } catch (Exception e) {
                                logger.error(String.format("Failed to read Keycloak definition %s", path), e);
                                throw e;
                            }

                            if (keycloakDefinition != null) {
                                results.put(path.toString(), keycloakDefinition);
                            }
                        } else {
                            logger.debug("Skipping {}", path);
                        }

                        return FileVisitResult.CONTINUE;
                    }
                }
        );

        return results;
    }

    private KeycloakDefinition validateKeycloakDefinitions(
            KeycloakRestApi keycloakRestApi,
            Map<String, KeycloakDefinition> keycloakDefinitionMap
    ) {
        logger.info("Validating Keycloak Definitions");

        boolean deleteManagedResourcesWhenRemoved = !keycloakDefinitionMap.isEmpty();
        Map<String, Pair<String, RealmDefinition>> realms = new HashMap<>();
        Map<String, Pair<String, ClientDefinition>> clients = new HashMap<>();
        Map<String, Pair<String, AuthenticationFlowDefinition>> authenticationFlows = new HashMap<>();

        // Removed resources will only be deleted of all the KeycloakDefinitions have deleteManagedResourcesWhenRemoved set to true.
        for(KeycloakDefinition keycloakDefinition:keycloakDefinitionMap.values()) {
            deleteManagedResourcesWhenRemoved = deleteManagedResourcesWhenRemoved && Boolean.TRUE.equals(keycloakDefinition.getDeleteManagedResourcesWhenRemoved());
        }

        // Process all the Platform Role Definitions
        boolean allValid = this.realmConfigurer.validateDefinitions(
                keycloakDefinitionMap,
                realms
        );

        // Process all the Authentication Flows Definitions
        allValid = allValid & this.authenticationFlowConfigurer.validateDefinitions(
                keycloakDefinitionMap,
                authenticationFlows
        );

        // Process all the Client Definitions
        allValid = allValid & this.clientConfigurer.validateDefinitions(
                keycloakDefinitionMap,
                clients
        );

        allValid = allValid & crossValidation(keycloakRestApi, keycloakDefinitionMap);

        KeycloakDefinition result = null;
        
        if (allValid) {
            logger.info("Keycloak Definitions are valid");
            
            result = Pair.createKeycloakDefinition(
                    deleteManagedResourcesWhenRemoved,
                    realms.values(),
                    authenticationFlows.values(),
                    clients.values()
            );
        } else {
            logger.error("Keycloak Definitions are not valid");
        }
        
        return result;
    }

    private boolean crossValidation(
            KeycloakRestApi keycloakRestApi,
            Map<String, KeycloakDefinition> keycloakDefinitionMap
    ) {
        boolean valid = true;

        Map<String, List<AuthenticationFlowDefinition>> flowDefinitionsByRealm = getAuthenticationFlowDefinitionMap(keycloakDefinitionMap);

        Map<String, List<ClientDefinition>> clientDefinitionsByRealm = getClientDefinitionMap(keycloakDefinitionMap);

        for (String path:keycloakDefinitionMap.keySet()) {

            KeycloakDefinition keycloakDefinition = keycloakDefinitionMap.get(path);

            for (int index = 0; index < keycloakDefinition.getRealms().size(); ++index) {

                RealmDefinition realmDefinition = keycloakDefinition.getRealms().get(index);

                String realmName = realmDefinition.getRealmName();

                List<AuthenticationFlowDefinition> flowDefinitions = flowDefinitionsByRealm.get(realmName);
                if (flowDefinitions == null) {
                    flowDefinitions = new ArrayList<>();
                }

                List<ClientDefinition> clientDefinitions = clientDefinitionsByRealm.get(realmName);
                if (clientDefinitions == null) {
                    clientDefinitions = new ArrayList<>();
                }

                List<Flow> existingFlows = keycloakRestApi.getTopLevelFlows(realmName);

                Map<String, Flow> existingFlowsById = existingFlows==null?new HashMap<>():existingFlows.stream()
                        .collect(Collectors.toMap(Flow::getId, Function.identity()));

                Map<String, Flow> existingFlowsByAlias = existingFlows==null?new HashMap<>():existingFlows.stream()
                        .collect(Collectors.toMap(Flow::getAlias, Function.identity()));

                Map<String, AuthenticationFlowDefinition> flowDefinitionsByAlias = flowDefinitions.stream()
                        .collect(Collectors.toMap(AuthenticationFlowDefinition::getAlias, Function.identity()));

                Map<String, ClientDefinition> clientDefinitionsByClientId = clientDefinitions.stream()
                        .collect(Collectors.toMap(ClientDefinition::getClientId, Function.identity()));

                Realm realm = keycloakRestApi.getRealmByName(realmName);

                if (valid) {

                    // An Authentication Flow cannot be removed if it is being used as a default
                    Map<String, String> defaultBindingsMap = getDefaultBindingsMap(realmDefinition, realm);

                    for (String flowReference: DefaultFlowBindingReferences) {

                        valid = valid & validateBinding(realmName, flowReference, defaultBindingsMap.get(flowReference), existingFlowsByAlias, flowDefinitionsByAlias);
                    }
                }

                if (valid) {

                    // An Authentication Flow cannot be removed if it is being used by an Identity Provider
                    Map<String, String> identityProviderBindings = getIdentityProviderBindingsMap(realm, realmDefinition);

                    for (String flowReference:identityProviderBindings.keySet()) {

                        valid = valid & validateBinding(realmName, flowReference, identityProviderBindings.get(flowReference), existingFlowsByAlias, flowDefinitionsByAlias);
                    }
                }

                if (valid) {

                    // An Authentication Flow cannot be removed if it is being used by a client
                    Map<String, String> clientBindings = getClientBindingsMap(keycloakRestApi, realm, existingFlowsById, clientDefinitionsByClientId);

                    for (String flowReference:clientBindings.keySet()) {

                        valid = valid & validateBinding(realmName, flowReference, clientBindings.get(flowReference), existingFlowsByAlias, flowDefinitionsByAlias);
                    }
                }
            }
        }

        return valid;
    }

    private Map<String, String> getClientBindingsMap(KeycloakRestApi keycloakRestApi, Realm realm, Map<String, Flow> existingFlowsById, Map<String, ClientDefinition> clientDefinitions) {
        Map<String, String> result = new HashMap<>();

        if(realm != null) {
            int pageOffset = 0;
            int pageSize = 10;
            int pageCount;
            do {
                List<Client> clients = keycloakRestApi.getClients(realm.getRealm(), pageOffset, pageOffset + pageSize);

                for (Client client : clients) {

                    ClientDefinition clientDefinition = clientDefinitions.get(client.getClientId());

                    // If the client is going to be overwritten or removed then we do not need to record the existing bindings
                    if (clientDefinition == null || !ClientConfigurer.isManaged(client)) {

                        Map<String, String> flowBindingOverrides = client.getAuthenticationFlowBindingOverrides();

                        String browserFlowId = flowBindingOverrides.get("browser");

                        if (browserFlowId != null && !browserFlowId.isBlank()) {

                            Flow flow = existingFlowsById.get(browserFlowId);

                            String flowReference = String.format("Client (%s) Browser Flow Override", client.getClientId());

                            if (flow == null) {

                                result.put(flowReference, INVALID);
                            } else {

                                result.put(flowReference, flow.getAlias());
                            }
                        }

                        String directGrantFlowId = flowBindingOverrides.get("direct_grant");

                        if (directGrantFlowId != null && !directGrantFlowId.isBlank()) {

                            Flow flow = existingFlowsById.get(directGrantFlowId);

                            String flowReference = String.format("Client (%s) Direct Grant Flow Override", client.getClientId());

                            if (flow == null) {

                                result.put(flowReference, INVALID);
                            } else {

                                result.put(flowReference, flow.getAlias());
                            }
                        }
                    }
                }

                pageCount = clients.size();
                pageOffset += pageCount;
            } while (pageCount >= pageSize);
        }

        for (ClientDefinition definition:clientDefinitions.values()) {

            ClientDefinition.GrantTypes grantTypes = definition.getGrantTypes();
            ClientDefinition.GrantTypes.AuthorizationCode authorizationCodeGrant = grantTypes.getAuthorizationCode();
            if (authorizationCodeGrant != null) {

                String browserFlowOverrideAlias = authorizationCodeGrant.getFlowOverride();

                if (browserFlowOverrideAlias != null && !browserFlowOverrideAlias.isBlank()) {

                    String flowReference = String.format("Client (%s) Browser Flow Override", definition.getClientId());

                    result.put(flowReference, browserFlowOverrideAlias);
                }
            }

            ClientDefinition.GrantTypes.Password passwordGrant = grantTypes.getPassword();
            if (passwordGrant != null) {

                String directGrantFlowOverrideAlias = passwordGrant.getFlowOverride();

                if (directGrantFlowOverrideAlias != null && !directGrantFlowOverrideAlias.isBlank()) {

                    String flowReference = String.format("Client (%s) Direct Grant Flow Override", definition.getClientId());

                    result.put(flowReference, directGrantFlowOverrideAlias);
                }
            }
        }

        return result;
    }

    private Map<String, String> getIdentityProviderBindingsMap(Realm realm, RealmDefinition realmDefinition) {
        Map<String, String> result = new HashMap<>();

        if (realm != null) {

            List<IdentityProvider> identityProviders = realm.getIdentityProviders();
            for (IdentityProvider identityProvider:identityProviders) {

                if (!GOOGLE_IDP_ALIAS.equals(identityProvider.getAlias())) {
                    // Exclude the Google IDP because it will be overridden by the definition
                    // Other IDPs are not managed by the configurer, so we can be sure that the flow references will not be changed or removed by this process

                    if (identityProvider.getFirstBrokerLoginFlowAlias()!=null && !identityProvider.getFirstBrokerLoginFlowAlias().isBlank()) {
                        result.put(String.format("IDP (%s) First Broker Login", identityProvider.getAlias()), identityProvider.getFirstBrokerLoginFlowAlias());
                    }

                    if (identityProvider.getPostBrokerLoginFlowAlias()!=null && !identityProvider.getPostBrokerLoginFlowAlias().isBlank()) {
                        result.put(String.format("IDP (%s) Post Broker Login", identityProvider.getAlias()), identityProvider.getPostBrokerLoginFlowAlias());
                    }
                }
            }
        }

        GoogleIdentityProvider googleIdpDefinition = realmDefinition.getGoogleIdentityProvider();
        if (googleIdpDefinition != null) {

            if (googleIdpDefinition.getFirstLoginFlowAlias()!=null && !googleIdpDefinition.getFirstLoginFlowAlias().isBlank()) {
                result.put(String.format("IDP (%s)", GOOGLE_IDP_ALIAS), googleIdpDefinition.getFirstLoginFlowAlias());
            }

            if (googleIdpDefinition.getPostLoginFlowAlias()!=null && !googleIdpDefinition.getPostLoginFlowAlias().isBlank()) {
                result.put(String.format("IDP (%s)", GOOGLE_IDP_ALIAS), googleIdpDefinition.getPostLoginFlowAlias());
            }
        }

        return result;
    }

    private boolean validateBinding(
            String realmName,
            String flowReference,
            String flowAlias,
            Map<String, Flow> existingFlows,
            Map<String, AuthenticationFlowDefinition> flowDefinitions
    ) {
        boolean valid = true;

        if (flowAlias == null || flowAlias.isBlank()) {
            throw new IllegalStateException(String.format("Not expecting a null flow alias for binding %s", flowReference));
        }

        if (INVALID.equals(flowAlias)) {

            logger.warn("Existing Invalid Authentication Flow binding alias '{}' for {} in realm {}", flowAlias, flowReference, realmName);
        } else if (!flowDefinitions.containsKey(flowAlias) && !DefaultFlowAliases.contains(flowAlias)) {

            Flow existingFlow = existingFlows.get(flowAlias);

            if (existingFlow == null || !Boolean.TRUE.equals(existingFlow.getBuiltIn())) {

                logger.error("Invalid Authentication Flow binding alias '{}' for {} in realm {}", flowAlias, flowReference, realmName);
                valid = false;
            }
        }

        return valid;
    }

    private Map<String, List<AuthenticationFlowDefinition>> getAuthenticationFlowDefinitionMap(
            Map<String, KeycloakDefinition> keycloakDefinitionMap
    ) {
        Map<String, List<AuthenticationFlowDefinition>> result = new HashMap<>();

        for(KeycloakDefinition keycloakDefinition:keycloakDefinitionMap.values()) {

            for (AuthenticationFlowDefinition authenticationFlowDefinition:keycloakDefinition.getAuthenticationFlows()) {

                String realmName = authenticationFlowDefinition.getRealmName();
                List<AuthenticationFlowDefinition> realmList = result.computeIfAbsent(realmName, k -> new ArrayList<>());
                realmList.add(authenticationFlowDefinition);
            }
        }

        return result;
    }

    private Map<String, List<ClientDefinition>> getClientDefinitionMap(
            Map<String, KeycloakDefinition> keycloakDefinitionMap
    ) {
        Map<String, List<ClientDefinition>> result = new HashMap<>();

        for(KeycloakDefinition keycloakDefinition:keycloakDefinitionMap.values()) {

            for (ClientDefinition clientDefinition:keycloakDefinition.getClients()) {

                String realmName = clientDefinition.getRealmName();
                List<ClientDefinition> realmList = result.computeIfAbsent(realmName, k -> new ArrayList<>());
                realmList.add(clientDefinition);
            }
        }

        return result;
    }

    private Map<String, String> getDefaultBindingsMap(RealmDefinition realmDefinition, Realm realm) {
        Map<String, String> result = new HashMap<>();

        if (realm == null) {
            // If the realm does not exist we can just use the known Keycloak defaults
            result.put(DefaultBrowserFlowBindingReference, DefaultBrowserFlowAlias);
            result.put(DefaultRegistrationFlowBindingReference, DefaultRegistrationFlowAlias);
            result.put(DefaultDirectGrantFlowBindingReference, DefaultDirectGrantFlowAlias);
            result.put(DefaultResetCredentialsFlowBindingReference, DefaultResetCredentialsFlowAlias);
            result.put(DefaultClientAuthenticationFlowBindingReference, DefaultClientAuthenticationFlowAlias);
        } else {
            result.put(DefaultBrowserFlowBindingReference, realm.getBrowserFlow());
            result.put(DefaultRegistrationFlowBindingReference, realm.getRegistrationFlow());
            result.put(DefaultDirectGrantFlowBindingReference, realm.getDirectGrantFlow());
            result.put(DefaultResetCredentialsFlowBindingReference, realm.getResetCredentialsFlow());
            result.put(DefaultClientAuthenticationFlowBindingReference, realm.getClientAuthenticationFlow());
        }

        String defaultBrowserFlowAlias = realmDefinition.getDefaultBrowserFlowAlias();
        if (defaultBrowserFlowAlias != null && !defaultBrowserFlowAlias.isBlank()) {
            result.put(DefaultBrowserFlowBindingReference, defaultBrowserFlowAlias);
        }

        String defaultRegistrationFlowAlias = realmDefinition.getDefaultRegistrationFlowAlias();
        if (defaultRegistrationFlowAlias != null && !defaultRegistrationFlowAlias.isBlank()) {
            result.put(DefaultRegistrationFlowBindingReference, defaultRegistrationFlowAlias);
        }

        String defaultDirectGrantFlowAlias = realmDefinition.getDefaultDirectGrantFlowAlias();
        if (defaultDirectGrantFlowAlias != null && !defaultDirectGrantFlowAlias.isBlank()) {
            result.put(DefaultDirectGrantFlowBindingReference, defaultDirectGrantFlowAlias);
        }

        String defaultResetCredentialsFlowAlias = realmDefinition.getDefaultResetCredentialsFlowAlias();
        if (defaultResetCredentialsFlowAlias != null && !defaultResetCredentialsFlowAlias.isBlank()) {
            result.put(DefaultResetCredentialsFlowBindingReference, defaultResetCredentialsFlowAlias);
        }

        String defaultClientAuthenticationFlowAlias = realmDefinition.getDefaultClientAuthenticationFlowAlias();
        if (defaultClientAuthenticationFlowAlias != null && !defaultClientAuthenticationFlowAlias.isBlank()) {
            result.put(DefaultClientAuthenticationFlowAlias, defaultClientAuthenticationFlowAlias);
        }

        return result;
    }

    /**
     * Update the current state of the realms, and service clients to match the provided definitions.
     *
     * @param keycloakDefinition the new platform security definition
     */
    private boolean applyKeycloakDefinition(
            KeycloakDefinition keycloakDefinition
    ) {

        boolean deleteManagedResourcesWhenRemoved = Boolean.TRUE.equals(keycloakDefinition.getDeleteManagedResourcesWhenRemoved());

        boolean updateApplied = realmConfigurer.applyRealms(keycloakDefinition, deleteManagedResourcesWhenRemoved);

        updateApplied = authenticationFlowConfigurer.applyAuthenticationFlows(keycloakDefinition) || updateApplied;

        updateApplied = this.clientConfigurer.applyClients(keycloakDefinition, deleteManagedResourcesWhenRemoved) || updateApplied;

        return updateApplied;
    }
}
