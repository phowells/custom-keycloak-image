package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.GoogleIdentityProvider;
import com.paulhowells.keycloak.configurer.model.IdentityProviderMapperDefinition;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.IdentityProvider;
import com.paulhowells.keycloak.configurer.rest.client.model.IdentityProviderMapper;
import com.paulhowells.keycloak.configurer.rest.client.model.Realm;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.paulhowells.keycloak.configurer.rest.client.model.IdentityProvider.GOOGLE_IDP_ALIAS;
import static com.paulhowells.keycloak.configurer.rest.client.model.IdentityProvider.GOOGLE_IDP_PROVIDER_ID;

public class GoolgeIdpConfigurer {
    private static final String MANAGED_BY_ATTRIBUTE_VALUE = "google-idp-configurer";

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    private final IdentityProviderMapperConfigurer identityProviderMapperConfigurer;

    GoolgeIdpConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger
    ) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
        this.identityProviderMapperConfigurer = new IdentityProviderMapperConfigurer(keycloakRestApi, logger);
    }

    boolean processUpdate(
            Realm realm,
            GoogleIdentityProvider definition,
            boolean deleteManagedResourcesWhenRemoved
    ) {
        logger.debug("<processUpdate");
        boolean result = false;

        IdentityProvider googleIdentityProvider = null;
        for (IdentityProvider identityProvider:realm.getIdentityProviders()) {
            if (GOOGLE_IDP_ALIAS.equals(identityProvider.getAlias())) {
                if (googleIdentityProvider==null) {
                    googleIdentityProvider = identityProvider;
                } else {
                    throw new IllegalStateException("Not expecting multiple google identity providers");
                }
            }
        }

        if (googleIdentityProvider == null) {

            if (definition != null) {

                createResource(
                        realm,
                        definition
                );
                result = true;
            }
        } else {

            if (definition == null) {

                result = removeResource(
                        googleIdentityProvider,
                        deleteManagedResourcesWhenRemoved
                );
            } else {

                result = updateResource(
                        googleIdentityProvider,
                        definition
                );
            }
        }

        logger.debug(">processUpdate {}", result);
        return result;
    }

    private boolean removeResource(
            IdentityProvider identityProvider,
            boolean deleteManagedResourcesWhenRemoved
    ) {
        logger.debug("<removeResource");
        boolean result = false;

        String mangedByAttribute = identityProvider.getConfig().getManagedBy();

        if (MANAGED_BY_ATTRIBUTE_VALUE.equals(mangedByAttribute)) {

            if (deleteManagedResourcesWhenRemoved) {

                logger.warn("Deleting {} Google IDP", identityProvider.getRealm());

                keycloakRestApi.deleteIdentityProvider(identityProvider.getRealm(), identityProvider.getAlias());
                result = true;
            } else if(Boolean.TRUE.equals(identityProvider.getEnabled())) {

                logger.info("Disabling {} Google IDP", identityProvider.getRealm());

                identityProvider.setEnabled(Boolean.FALSE);

                keycloakRestApi.updateIdentityProvider(identityProvider.getRealm(), identityProvider.getAlias(), identityProvider);
                result = true;
            }
        } else {

            logger.debug("{} Google ID not managed by {}", identityProvider.getRealm(), MANAGED_BY_ATTRIBUTE_VALUE);
        }

        logger.debug(">removeResource");
        return result;
    }

    GoogleIdentityProvider getDefinition(Realm realm) {

        List<IdentityProvider> identityProviders = realm.getIdentityProviders();
        IdentityProvider identityProvider = null;
        for (IdentityProvider tmp:identityProviders) {
            if (GOOGLE_IDP_ALIAS.equals(tmp.getAlias())) {
                if (identityProvider==null) {
                    identityProvider = tmp;
                } else {
                    throw new IllegalStateException("Not expecting multiple google identity providers");
                }
            }
        }

        return getDefinition(identityProvider);
    }

    private GoogleIdentityProvider getDefinition(IdentityProvider identityProvider) {
        GoogleIdentityProvider result = null;

        if (identityProvider!=null) {

            result = new GoogleIdentityProvider();

            result.setEnabled(identityProvider.getEnabled());
            result.setClientId(identityProvider.getConfig().getClientId());
            result.setClientSecret(identityProvider.getConfig().getClientSecret());
            String guiOrder = identityProvider.getConfig().getGuiOrder();
            if (guiOrder!=null) {
                result.setDisplayOrder(Long.valueOf(guiOrder));
            }
            List<String> hostedDomains = new ArrayList<>();
            String hostedDomain = identityProvider.getConfig().getHostedDomain();
            if (hostedDomain!=null&&!hostedDomain.isBlank()) {
                hostedDomains.addAll(Arrays.asList(hostedDomain.split(",")));
            }
            result.setHostedDomains(hostedDomains);
            result.setUseUserIpParam(Boolean.valueOf(identityProvider.getConfig().getUserIp()));
            // This is an unintuitive mapping but has been validated
            result.setRequestRefreshToken(Boolean.valueOf(identityProvider.getConfig().getOfflineAccess()));
            List<String> scopes = new ArrayList<>();
            String defaultScope = identityProvider.getConfig().getDefaultScope();
            if (defaultScope!=null&&!defaultScope.isBlank()) {
                scopes.addAll(Arrays.asList(defaultScope.split(" ")));
            }
            result.setScopes(scopes);
            result.setStoreTokens(Boolean.TRUE.equals(identityProvider.getStoreToken()));
            result.setAcceptsPromptNoneForward(Boolean.valueOf(identityProvider.getConfig().getAcceptsPromptNoneForwardFromClient()));
            result.setDisableUserInfo(Boolean.valueOf(identityProvider.getConfig().getDisableUserInfo()));
            result.setTrustEmail(Boolean.TRUE.equals(identityProvider.getTrustEmail()));
            result.setAccountLinkingOnly(Boolean.TRUE.equals(identityProvider.getLinkOnly()));
            result.setHideOnLoginPage(Boolean.valueOf(identityProvider.getConfig().getHideOnLoginPage()));
            boolean filteredByClaim = Boolean.parseBoolean(identityProvider.getConfig().getFilteredByClaim());
            if (filteredByClaim) {
                result.getEssentialClaim().setEnabled(Boolean.TRUE);
                result.getEssentialClaim().setClaimName(identityProvider.getConfig().getClaimFilterName());
                result.getEssentialClaim().setRegex(identityProvider.getConfig().getClaimFilterValue());
            }
            result.setFirstLoginFlowAlias(identityProvider.getFirstBrokerLoginFlowAlias());
            result.setPostLoginFlowAlias(identityProvider.getPostBrokerLoginFlowAlias());
            result.setSyncMode(identityProvider.getConfig().getSyncMode());

            List<IdentityProviderMapper> mappers = keycloakRestApi.getIdentityProviderMappers(identityProvider.getRealm(), identityProvider.getAlias());
            List<IdentityProviderMapperDefinition> mapperDefinitions = this.identityProviderMapperConfigurer.getDefinitions(mappers);
            result.setMappers(mapperDefinitions);
        }

        return result;
    }

    private boolean updateResource(
            IdentityProvider googleIdentityProvider,
            GoogleIdentityProvider definition
    ) {
        logger.debug("<updateResource");
        boolean result = false;

        GoogleIdentityProvider current = getDefinition(
                googleIdentityProvider
        );

        logger.info("Checking {} Google IDP for updates", googleIdentityProvider.getRealm());

        if (isDirty(current, definition)) {

            logger.info("Updating {} Google IDP", googleIdentityProvider.getRealm());

            // Apply changes to resource
            applyDefinition(
                    definition,
                    googleIdentityProvider
            );

            googleIdentityProvider.getConfig().setManagedBy(MANAGED_BY_ATTRIBUTE_VALUE);

            keycloakRestApi.updateIdentityProvider(
                    googleIdentityProvider.getRealm(),
                    GOOGLE_IDP_ALIAS,
                    googleIdentityProvider
            );
            result = true;

            this.identityProviderMapperConfigurer.applyIdentityProviderMappers(
                    googleIdentityProvider.getRealm(),
                    GOOGLE_IDP_ALIAS,
                    definition.getMappers()
            );
        } else {

            logger.info("No Change");
        }

        logger.debug(">updateResource {}", result);
        return result;
    }

    private void createResource(
            Realm realm,
            GoogleIdentityProvider definition
    ) {
        logger.debug("<createResource");

        logger.info("Creating {} Google IDP", realm.getRealm());

        IdentityProvider googleIdentityProvider = new IdentityProvider();

        applyDefinition(
                definition,
                googleIdentityProvider
        );

        googleIdentityProvider.getConfig().setManagedBy(MANAGED_BY_ATTRIBUTE_VALUE);

        keycloakRestApi.addIdentityProvider(
                realm.getRealm(),
                googleIdentityProvider
        );

        this.identityProviderMapperConfigurer.applyIdentityProviderMappers(
                realm.getRealm(),
                GOOGLE_IDP_ALIAS,
                definition.getMappers()
        );

        logger.debug(">createResource");
    }

    private void applyDefinition(
            GoogleIdentityProvider definition,
            IdentityProvider identityProvider
    ) {

        identityProvider.setProviderId(GOOGLE_IDP_PROVIDER_ID);
        identityProvider.setAlias(GOOGLE_IDP_ALIAS);
        identityProvider.setEnabled(definition.getEnabled());

        IdentityProvider.Config config = identityProvider.getConfig();
        config.setClientId(definition.getClientId());
        config.setClientSecret(definition.getClientSecret());
        Long displayOrder = definition.getDisplayOrder();
        if (displayOrder == null) {
            config.setGuiOrder(null);
        } else {
            config.setGuiOrder(displayOrder.toString());
        }
        List<String> hostedDomainList = definition.getHostedDomains();
        String hostedDomain = String.join(",", hostedDomainList);
        config.setHostedDomain(hostedDomain);
        Boolean useUserIpParam = definition.getUseUserIpParam();
        if (useUserIpParam==null) {
            config.setUserIp(null);
        } else {
            config.setUserIp(useUserIpParam.toString());
        }
        // This is an unintuitive mapping but has been validated
        Boolean requestRefreshToken = definition.getRequestRefreshToken();
        if (requestRefreshToken==null) {
            config.setOfflineAccess(null);
        } else {
            config.setOfflineAccess(requestRefreshToken.toString());
        }
        List<String> scopesList = definition.getScopes();
        String defaultScope = String.join(" ", scopesList);
        config.setDefaultScope(defaultScope);
        identityProvider.setStoreToken(definition.getStoreTokens());
        Boolean acceptsPromptNoneForward = definition.getAcceptsPromptNoneForward();
        if (acceptsPromptNoneForward==null) {
            config.setAcceptsPromptNoneForwardFromClient(null);
        } else {
            config.setAcceptsPromptNoneForwardFromClient(acceptsPromptNoneForward.toString());
        }
        Boolean disableUserInfo = definition.getDisableUserInfo();
        if (disableUserInfo==null) {
            config.setDisableUserInfo(null);
        } else {
            config.setDisableUserInfo(disableUserInfo.toString());
        }
        identityProvider.setTrustEmail(definition.getTrustEmail());
        identityProvider.setLinkOnly(definition.getAccountLinkingOnly());
        Boolean hideOnLoginPage = definition.getHideOnLoginPage();
        if (hideOnLoginPage==null) {
            config.setHideOnLoginPage(null);
        } else {
            config.setHideOnLoginPage(hideOnLoginPage.toString());
        }
        GoogleIdentityProvider.EssentialClaim essentialClaim = definition.getEssentialClaim();
        if(essentialClaim==null||!Boolean.TRUE.equals(essentialClaim.getEnabled())) {
            config.setFilteredByClaim(Boolean.FALSE.toString());
            config.setClaimFilterName(null);
            config.setClaimFilterValue(null);
        } else {
            config.setFilteredByClaim(Boolean.TRUE.toString());
            config.setClaimFilterName(essentialClaim.getClaimName());
            config.setClaimFilterValue(essentialClaim.getRegex());
        }
        identityProvider.setFirstBrokerLoginFlowAlias(definition.getFirstLoginFlowAlias());
        identityProvider.setPostBrokerLoginFlowAlias(definition.getPostLoginFlowAlias());
        config.setSyncMode(definition.getSyncMode());
    }

    private boolean isDirty(
            GoogleIdentityProvider current,
            GoogleIdentityProvider updated) {
        return !current.isUnchanged(updated, null, logger);
    }
}
