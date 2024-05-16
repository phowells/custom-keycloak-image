package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.AuthenticationFlowDefinition;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.Config;
import com.paulhowells.keycloak.configurer.rest.client.model.Flow;
import com.paulhowells.keycloak.configurer.rest.client.model.FlowExecution;
import org.slf4j.Logger;

import java.util.*;

public class AuthenticationFlowExecutionConfigurer {

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    AuthenticationFlowExecutionConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
    }

    void applyFlowExecutions(
            Flow parentFlow,
            List<AuthenticationFlowDefinition.Execution> executionDefinitions
    ) {

        List<AuthenticationFlowDefinition.Execution> insertDefinitions = new ArrayList<>();
        Map<String, AuthenticationFlowDefinition.Execution> updateDefinitionMap = new HashMap<>();
        Set<FlowExecution> referencedResources = new HashSet<>();
        Map<String, FlowExecution> existingResourceMap = getResourceMap(parentFlow);

        // Identify if the realm is new or if a realm needs to be updated
        // Keep track of which realms are in use, so we know which ones need to be deleted later
        identifyUpdates(
                existingResourceMap,
                parentFlow.getId(),
                executionDefinitions,
                insertDefinitions,
                updateDefinitionMap,
                referencedResources
        );

        List<String> referencedResourceKeys = AuthenticationFlowExecutionConfigurer.getReferencedResourceKeys(parentFlow.getId(), referencedResources);
        Map<String, FlowExecution> removedResourceMap = AuthenticationFlowExecutionConfigurer.getRemovedResourceMap(existingResourceMap, referencedResourceKeys);

        processUpdates(
                parentFlow,
                removedResourceMap,
                insertDefinitions,
                updateDefinitionMap
        );
    }

    private Map<String, FlowExecution> getResourceMap(
            Flow parentFlow
    ) {
        Map<String, FlowExecution> results = new HashMap<>();

        List<FlowExecution> flowExecutions = keycloakRestApi.getFlowExecutions(parentFlow);

        for (FlowExecution flowExecution:flowExecutions) {

            String key = getKey(parentFlow.getId(), flowExecution.getDisplayName());

            results.put(key, flowExecution);
        }

        return results;
    }

    private void identifyUpdates(
            Map<String, FlowExecution> existingResourceMap,
            String parentFlowId,
            List<AuthenticationFlowDefinition.Execution> executionDefinitions,
            List<AuthenticationFlowDefinition.Execution> insertDefinitions,
            Map<String, AuthenticationFlowDefinition.Execution> updateDefinitionsPkMap,
            Set<FlowExecution> referencedResources
    ) {

        // Identify if the definition is new or if an existing definition needs to be updated
        // Keep track of which definitions are in use, so we know which ones need to be deleted later
        for (AuthenticationFlowDefinition.Execution definition: executionDefinitions) {
            
            String key = getKey(parentFlowId, definition.getDisplayName());

            FlowExecution existingResource = existingResourceMap.get(key);
            if (existingResource == null) {

                insertDefinitions.add(definition);
            } else {

                definition.setId(existingResource.getId());
                updateDefinitionsPkMap.put(key, definition);
                referencedResources.add(existingResource);
            }
        }
    }

    private void updateFlowExecutionPriorities(
            Flow parentFlow,
            List<AuthenticationFlowDefinition.Execution> executionDefinitions
    ) {
        boolean reordered;
        do {
            reordered = false;

            List<FlowExecution> flowExecutions = keycloakRestApi.getFlowExecutions(parentFlow);

            if (flowExecutions.size() != executionDefinitions.size()) {
                throw new IllegalStateException("Not expecting the Flow Executions to out of sync with the Execution definitions");
            }

            if (logger.isDebugEnabled()) {

                logger.info("Re-ordering executions {} ({})", parentFlow.getRealm(), parentFlow.getAlias());
                logger.info("Current Order");
                int i = 0;
                for (FlowExecution flowExecution : flowExecutions) {
                    logger.info("{} ({}) {}", ++i, parentFlow.getAlias(), flowExecution.getDisplayName());
                }
                logger.info("Desired Order");
                i = 0;
                for (AuthenticationFlowDefinition.Execution executionDefinition : executionDefinitions) {
                    logger.info("{} ({}) {}", ++i, parentFlow.getAlias(), executionDefinition.getDisplayName());
                }
            }

            Map<String, FlowExecution> flowExecutionMap = new HashMap<>();
            for (FlowExecution flowExecution : flowExecutions) {
                flowExecutionMap.put(flowExecution.getDisplayName(), flowExecution);
            }

            for (AuthenticationFlowDefinition.Execution executionDefinition : executionDefinitions) {

                FlowExecution flowExecution = flowExecutionMap.get(executionDefinition.getDisplayName());

                if (flowExecution == null) {
                    throw new IllegalStateException(String.format("Not expecting a missing FlowExecution: %s", executionDefinition.getDisplayName()));
                }

                int currentIndex = flowExecution.getIndex();
                int requiredIndex = executionDefinition.getIndex();
                if (currentIndex < requiredIndex) {

                    throw new IllegalStateException(String.format("Not expecting the current index (%s) to be less than the required index (%s)", currentIndex, requiredIndex));
                } else if (currentIndex > requiredIndex) {

                    for (int i=currentIndex;i!=requiredIndex;--i) {

                        keycloakRestApi.raiseFlowExecutionPriority(parentFlow.getRealm(), flowExecution.getId());
                    }
                    reordered = true;
                    break;
                }
            }
        } while (reordered);
    }

    private void processUpdates(
            Flow parentFlow,
            Map<String, FlowExecution> removedResourceMap,
            List<AuthenticationFlowDefinition.Execution> insertDefinitions,
            Map<String, AuthenticationFlowDefinition.Execution> updateDefinitionMap
    ) {
        boolean updated = false;

        // Delete the obsolete  Flows
        for (String key:removedResourceMap.keySet()) {

            FlowExecution resource = removedResourceMap.get(key);

            updated = removeResource(parentFlow, resource) | updated;
        }

        // Insert the new  Flows
        for (AuthenticationFlowDefinition.Execution definition:insertDefinitions) {

            createResource(
                    parentFlow,
                    definition
            );
            updated = true;
        }

        // Update the existing  Flows
        for (AuthenticationFlowDefinition.Execution definition:updateDefinitionMap.values()) {

            updated = updateResource(
                    parentFlow,
                    definition
            ) | updated;
        }

        // Update priorities
        if (updated) {

            List<AuthenticationFlowDefinition.Execution> executionDefinitions = new ArrayList<>();
            executionDefinitions.addAll(insertDefinitions);
            executionDefinitions.addAll(updateDefinitionMap.values());
            executionDefinitions.sort(Comparator.comparingInt(AuthenticationFlowDefinition.Execution::getIndex));

            updateFlowExecutionPriorities(parentFlow, executionDefinitions);
        }
    }    
    
    private boolean removeResource(
            Flow parentFlow,
            FlowExecution resource
    ) {
        logger.debug("<removeResource");

        if (resource.getFlowId() != null) {

            // We need to delete the children so that we do not leave behind orphaned records which may cause problems later
            Flow subFlow = keycloakRestApi.getSubFlow(parentFlow, resource.getFlowId());

            applyFlowExecutions(subFlow, new ArrayList<>());
        }

        if (resource.getAuthenticationConfig() != null) {

            // We need to delete the children so that we do not leave behind orphaned records which may cause problems later
            keycloakRestApi.deleteConfig(parentFlow.getRealm(), resource.getAuthenticationConfig());
        }

        logger.warn("Deleting {} flow execution {} {}", parentFlow.getRealm(), parentFlow.getAlias(), resource.getDisplayName());

        keycloakRestApi.deleteFlowExecution(parentFlow.getRealm(), resource.getId());

        logger.debug(">removeResource");
        return true;
    }

    private void createResource(
            Flow parentFlow,
            AuthenticationFlowDefinition.Execution definition
    ) {
        switch(definition.getType()) {
            case "flow": {
                createResource(
                        parentFlow,
                        (AuthenticationFlowDefinition.Execution.Flow)definition
                );
                break;
            }
            case "authenticator": {
                assert definition instanceof AuthenticationFlowDefinition.Execution.Authenticator;
                createResource(
                        parentFlow,
                        (AuthenticationFlowDefinition.Execution.Authenticator)definition
                );
                break;
            }
            default: throw new IllegalArgumentException(String.format("Unsupported FlowExecution type: %s", definition.getType()));
        }
    }

    private void createResource(
            Flow parentFlow,
            AuthenticationFlowDefinition.Execution.Flow definition
    ) {
        logger.debug("<createResource");

        logger.info("Creating {} flow execution sub-flow      ({}) {}", parentFlow.getRealm(), parentFlow.getAlias(), definition.getDisplayName());

        String subFlowId = keycloakRestApi.createFlowExecutionSubFlow(
                parentFlow,
                definition.getProviderId(),
                definition.getDisplayName(),
                definition.getDescription(),
                definition.getRequirement()
        );

        Flow subFlow = keycloakRestApi.getSubFlow(parentFlow, subFlowId);

        // The flow id is returned when creating a flow execution instead of the execution id so we need to search the list of flow executions to find out new flow execution
        FlowExecution resource = keycloakRestApi.getFlowExecutions(parentFlow).stream().filter(t -> subFlowId.equals(t.getFlowId())).findFirst().get();

        // The requirement cannot be set during create so we have to follow up with an update
        resource.setRequirement(definition.getRequirement());
        keycloakRestApi.updateFlowExecution(
                parentFlow,
                resource
        );

        applyFlowExecutions(
                subFlow,
                definition.getExecutions()
        );

        logger.debug(">createResource");
    }

    private void createResource(
            Flow parentFlow,
            AuthenticationFlowDefinition.Execution.Authenticator definition
    ) {
        logger.debug("<createResource");

        logger.info("Creating {} flow execution authenticator ({}) {}", parentFlow.getRealm(), parentFlow.getAlias(), definition.getDisplayName());

        String id = keycloakRestApi.createFlowExecutionAuthenticator(
                parentFlow,
                definition.getProviderId(),
                definition.getDisplayName(),
                definition.getRequirement()
        );

        FlowExecution resource = keycloakRestApi.getFlowExecution(parentFlow.getRealm(), id);

        // The requirement cannot be set during create so we have to follow up with an update
        resource.setRequirement(definition.getRequirement());
        keycloakRestApi.updateFlowExecution(
                parentFlow,
                resource
        );

        processUpdates(
                parentFlow.getRealm(),
                definition,
                resource
        );

        resource = keycloakRestApi.getFlowExecution(parentFlow.getRealm(), id);

        logger.debug(">createResource {}", resource);
    }

    private boolean updateResource(
            Flow parentFlow,
            AuthenticationFlowDefinition.Execution definition
    ) {

        return switch (definition.getType()) {
            case "flow" -> updateResource(
                    parentFlow,
                    (AuthenticationFlowDefinition.Execution.Flow) definition
            );
            case "authenticator" -> {
                assert definition instanceof AuthenticationFlowDefinition.Execution.Authenticator;
                yield updateResource(
                        parentFlow,
                        (AuthenticationFlowDefinition.Execution.Authenticator) definition
                );
            }
            default ->
                    throw new IllegalArgumentException(String.format("Unsupported FlowExecution type: %s", definition.getType()));
        };
    }

    private boolean updateResource(
            Flow parentFlow,
            AuthenticationFlowDefinition.Execution.Flow definition
    ) {
        logger.debug("<updateResource");
        boolean result = false;

        FlowExecution resource = keycloakRestApi.getFlowExecution(parentFlow.getRealm(), definition.getId());

        AuthenticationFlowDefinition.Execution.Flow current = getExecutionSubFlowDefinition(
                parentFlow,
                resource
        );

        logger.info("Checking {} flow execution sub-flow      ({}) {} for updates", parentFlow.getRealm(), parentFlow.getAlias(), definition.getDisplayName());

        if (isDirty(current, definition)) {

            logger.info("Updating {} flow execution sub-flow      ({}) {}", parentFlow.getRealm(), parentFlow.getAlias(), definition.getDisplayName());

            // Apply changes to resource
            applyDefinition(
                    definition,
                    resource
            );

            keycloakRestApi.updateFlowExecution(
                    parentFlow,
                    resource
            );
            result = true;

            Flow subFlow = keycloakRestApi.getSubFlow(parentFlow, resource.getFlowId());

            applyFlowExecutions(
                    subFlow,
                    definition.getExecutions()
            );
        } else {

            logger.info("No Change");
        }

        logger.debug(">updateResource {}", result);
        return result;
    }

    private boolean updateResource(
            Flow parentFlow,
            AuthenticationFlowDefinition.Execution.Authenticator definition
    ) {
        logger.debug("<updateResource");
        boolean result = false;

        FlowExecution resource = keycloakRestApi.getFlowExecution(parentFlow.getRealm(), definition.getId());

        AuthenticationFlowDefinition.Execution.Authenticator current = getExecutionAuthenticatorDefinition(
                parentFlow.getRealm(),
                resource
        );

        logger.info("Checking {} flow execution authenticator ({}) {} for updates", parentFlow.getRealm(), parentFlow.getAlias(), definition.getDisplayName());

        if (isDirty(current, definition)) {

            logger.info("Updating {} flow execution authenticator ({}) {}", parentFlow.getRealm(), parentFlow.getAlias(), definition.getDisplayName());

            // Apply changes to resource
            applyDefinition(
                    definition,
                    resource
            );

            keycloakRestApi.updateFlowExecution(
                    parentFlow,
                    resource
            );
            result = true;

            processUpdates(
                    parentFlow.getRealm(),
                    definition,
                    resource
            );
        } else {

            logger.info("No Change");
        }

        logger.debug(">updateResource {}", result);
        return result;
    }

    private void processUpdates(
            String realmName,
            AuthenticationFlowDefinition.Execution.Authenticator definition,
            FlowExecution resource
    ) {

        Map<String, Object> configDefinition = definition.getConfig();
        String configId = resource.getAuthenticationConfig();
        if (configId == null) {
            if (!configDefinition.isEmpty()) {

                String name = (String) configDefinition.get("name");
                if (name == null || name.isBlank()) {
                    name = definition.getDisplayName();
                }

                Config config = new Config();
                config.setAlias(name);
                config.getConfig().putAll(configDefinition);
                config.getConfig().remove("name");

                keycloakRestApi.createConfig(
                        realmName,
                        resource.getId(),
                        config
                );
            }
        } else {
            if (configDefinition.isEmpty()) {
                keycloakRestApi.deleteConfig(
                        realmName,
                        configId
                );
            } else {

                Config config = keycloakRestApi.getConfig(realmName, configId);

                String name = (String) configDefinition.get("name");
                if (name == null || name.isBlank()) {
                    name = config.getAlias();
                }

                config.setAlias(name);
                config.getConfig().clear();
                config.getConfig().putAll(configDefinition);
                config.getConfig().remove("name");

                keycloakRestApi.updateConfig(
                        realmName,
                        configId,
                        config
                );
            }
        }
    }

    private AuthenticationFlowDefinition.Execution.Flow getExecutionSubFlowDefinition(
            Flow parentFlow,
            FlowExecution flowExecution
    ) {
        AuthenticationFlowDefinition.Execution.Flow definition = new AuthenticationFlowDefinition.Execution.Flow();

        definition.setProviderId(flowExecution.getProviderId());
        definition.setDisplayName(flowExecution.getDisplayName());
        definition.setRequirement(flowExecution.getRequirement());
        definition.setDescription(flowExecution.getDescription());

        Flow subFlow = this.keycloakRestApi.getSubFlow(parentFlow, flowExecution.getFlowId());

        List<AuthenticationFlowDefinition.Execution> executions = getExecutionDefinitions(subFlow);
        definition.getExecutions().addAll(executions);

        // Ensure that the executions are in the correct order
        definition.getExecutions().sort(Comparator.comparingInt(AuthenticationFlowDefinition.Execution::getIndex));

        return definition;
    }

    private AuthenticationFlowDefinition.Execution.Authenticator getExecutionAuthenticatorDefinition(
            String realmName,
            FlowExecution resource) {
        AuthenticationFlowDefinition.Execution.Authenticator definition = new AuthenticationFlowDefinition.Execution.Authenticator();

        definition.setProviderId(resource.getProviderId());
        definition.setDisplayName(resource.getDisplayName());
        definition.setRequirement(resource.getRequirement());
        String configId = resource.getAuthenticationConfig();
        if (configId!=null) {
            Config config = keycloakRestApi.getConfig(realmName, configId);
            definition.getConfig().put("name", config.getAlias());
            definition.getConfig().putAll(config.getConfig());
        }

        return definition;
    }

    List<AuthenticationFlowDefinition.Execution> getExecutionDefinitions(
            Flow parentFlow
    ) {
        List<AuthenticationFlowDefinition.Execution> results = new ArrayList<>();

        List<FlowExecution> flowExecutions = keycloakRestApi.getFlowExecutions(parentFlow);
        Map<String, FlowExecution> flowExecutionMap = new HashMap<>();
        Map<String, FlowExecution> authenticatorExecutionMap = new HashMap<>();
        for (FlowExecution flowExecution:flowExecutions) {
            flowExecutionMap.put(flowExecution.getDisplayName(), flowExecution);
            String key = String.format("%s:%s", flowExecution.getProviderId(), flowExecution.getAlias());
            authenticatorExecutionMap.put(key, flowExecution);
        }

        for (Flow.AuthenticationExecution authenticationExecution:parentFlow.getAuthenticationExecutions()) {

            if (Boolean.TRUE.equals(authenticationExecution.getAuthenticatorFlow())) {

                FlowExecution flowExecution = flowExecutionMap.get(authenticationExecution.getFlowAlias());
                AuthenticationFlowDefinition.Execution.Flow execution = new AuthenticationFlowDefinition.Execution.Flow();
                execution.setDisplayName(flowExecution.getDisplayName());
                execution.setDescription(flowExecution.getDescription());
                execution.setRequirement(flowExecution.getRequirement());
                execution.setIndex(flowExecution.getIndex());
                String subFlowId = flowExecution.getFlowId();

                Flow subFlow = keycloakRestApi.getSubFlow(parentFlow, subFlowId);

                execution.setProviderId(subFlow.getProviderId());

                List<AuthenticationFlowDefinition.Execution> executions = getExecutionDefinitions(subFlow);

                execution.getExecutions().addAll(executions);

                execution.getExecutions().sort(Comparator.comparingInt(AuthenticationFlowDefinition.Execution::getIndex));

                results.add(execution);
            } else {
                String key = String.format("%s:%s", authenticationExecution.getAuthenticator(), authenticationExecution.getAuthenticatorConfig());
                FlowExecution flowExecution = authenticatorExecutionMap.get(key);


                AuthenticationFlowDefinition.Execution.Authenticator execution = new AuthenticationFlowDefinition.Execution.Authenticator();
                execution.setProviderId(flowExecution.getProviderId());
                execution.setRequirement(flowExecution.getRequirement());
                execution.setIndex(flowExecution.getIndex());
                execution.setDisplayName(flowExecution.getDisplayName());

                String configId = flowExecution.getAuthenticationConfig();
                if (configId!=null) {
                    Config config = keycloakRestApi.getConfig(parentFlow.getRealm(), configId);
                    execution.getConfig().put("name", config.getAlias());
                    execution.getConfig().putAll(config.getConfig());
                }

                results.add(execution);
            }
        }

        return results;
    }

    private void applyDefinition(
            AuthenticationFlowDefinition.Execution.Flow definition,
            FlowExecution resource
    ) {
        resource.setRequirement(definition.getRequirement());
        resource.setDescription(definition.getDescription());
    }

    private void applyDefinition(
            AuthenticationFlowDefinition.Execution.Authenticator definition,
            FlowExecution resource
    ) {
        resource.setRequirement(definition.getRequirement());
    }

    private boolean isDirty(
            AuthenticationFlowDefinition.Execution.Flow current,
            AuthenticationFlowDefinition.Execution.Flow updated
    ) {
        return !current.isUnchanged(updated, null, logger);
    }

    private boolean isDirty(
            AuthenticationFlowDefinition.Execution.Authenticator current,
            AuthenticationFlowDefinition.Execution.Authenticator updated
    ) {
        return !current.isUnchanged(updated, null, logger);
    }

    private static Map<String, FlowExecution> getRemovedResourceMap(Map<String, FlowExecution> existingResourceMap, List<String> referencedKeys) {
        Map<String, FlowExecution> result = new HashMap<>();

        for (String key: existingResourceMap.keySet()) {

            // If the resource is not referenced
            if (!referencedKeys.contains(key)) {

                result.put(key, existingResourceMap.get(key));
            }
        }

        return result;
    }

    private static List<String> getReferencedResourceKeys(String parentFlowId, Set<FlowExecution> referencedResources) {
        return referencedResources.stream().map(it -> getKey(parentFlowId, it.getDisplayName())).toList();
    }

    private static String getKey(
            String flowId,
            String displayName
    ) {
        return String.format("%s:%s", flowId, displayName).toUpperCase();
    }
}
