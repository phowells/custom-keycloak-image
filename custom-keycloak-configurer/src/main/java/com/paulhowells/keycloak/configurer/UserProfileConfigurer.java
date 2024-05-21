package com.paulhowells.keycloak.configurer;

import com.paulhowells.keycloak.configurer.model.UserAttribute;
import com.paulhowells.keycloak.configurer.model.UserAttributeGroup;
import com.paulhowells.keycloak.configurer.model.UserProfile;
import com.paulhowells.keycloak.configurer.rest.client.KeycloakRestApi;
import com.paulhowells.keycloak.configurer.rest.client.model.Attribute;
import com.paulhowells.keycloak.configurer.rest.client.model.AttributeGroup;
import com.paulhowells.keycloak.configurer.rest.client.model.Profile;
import com.paulhowells.keycloak.configurer.rest.client.model.Realm;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class UserProfileConfigurer {

    private static final List<String> UNMANAGED_ATTRIBUTE_NAMES;
    static {
        UNMANAGED_ATTRIBUTE_NAMES = new ArrayList<>();
        UNMANAGED_ATTRIBUTE_NAMES.add("username");
        UNMANAGED_ATTRIBUTE_NAMES.add("email");
    }

    private final Logger logger;
    private final KeycloakRestApi keycloakRestApi;

    UserProfileConfigurer(
            KeycloakRestApi keycloakRestApi,
            Logger logger) {
        this.keycloakRestApi = keycloakRestApi;
        this.logger = logger;
    }

    boolean updateUserProfile(
            Realm realm,
            UserProfile definition
    ) {
        logger.debug("<updateUserProfile");
        boolean result = false;

        Profile userProfile = keycloakRestApi.getUserProfile(realm.getRealm());

        UserProfile current = getDefinition(userProfile);

        logger.info("Checking realm {} user profile for updates", realm.getRealm());

        if (isDirty(current, definition)) {

            logger.info("Updating realm {} user profile", realm.getRealm());

            applyDefinition(definition, userProfile);

            keycloakRestApi.updateUserProfile(realm.getRealm(), userProfile);
            result = true;

        } else {

            logger.info("No Change");
        }

        logger.debug(">updateUserProfile {}", result);
        return result;
    }

    private static boolean isManaged(Attribute resource) {

        return !UNMANAGED_ATTRIBUTE_NAMES.contains(resource.getName());
    }

    private boolean isDirty(
            UserProfile current,
            UserProfile updated) {
        return !current.isUnchanged(updated, null, logger);
    }

    UserProfile getDefinition(Realm realm) {

        Profile userProfile = keycloakRestApi.getUserProfile(realm.getRealm());

        return getDefinition(userProfile);
    }

    private UserProfile getDefinition(Profile userProfile) {
        UserProfile result = new UserProfile();

        for(Attribute attribute:userProfile.getAttributes()) {

            if (isManaged(attribute)) {
                UserAttribute userAttribute = new UserAttribute();

                userAttribute.setName(attribute.getName());
                userAttribute.setDisplayName(attribute.getDisplayName());
                userAttribute.setGroup(attribute.getGroup());
                userAttribute.setMultivalued(Boolean.TRUE.equals(attribute.getMultivalued()));
                userAttribute.setValidations(attribute.getValidations());
                UserAttribute.Permissions permissions = new UserAttribute.Permissions();
                if (attribute.getPermissions() != null) {
                    permissions.setView(attribute.getPermissions().getView());
                    permissions.setEdit(attribute.getPermissions().getEdit());
                }
                userAttribute.setPermissions(permissions);
                UserAttribute.Required required = new UserAttribute.Required();
                if (attribute.getRequired() != null) {
                    required.setRoles(attribute.getRequired().getRoles());
                    required.setScopes(attribute.getRequired().getScopes());
                }
                userAttribute.setRequired(required);

                result.getAttributes().put(userAttribute.getName(), userAttribute);
            }
        }

        for(AttributeGroup attributeGroup:userProfile.getGroups()) {

            UserAttributeGroup userAttributeGroup = new UserAttributeGroup();

            userAttributeGroup.setName(attributeGroup.getName());
            userAttributeGroup.setDisplayHeader(attributeGroup.getDisplayHeader());
            userAttributeGroup.setDisplayDescription(attributeGroup.getDisplayDescription());

            result.getGroups().put(userAttributeGroup.getName(), userAttributeGroup);
        }

        return result;
    }

    private void applyDefinition(UserProfile definition, Profile userProfile) {

        applyUserAttributes(definition, userProfile);
        applyUserAttributeGroups(definition, userProfile);
    }

    private void applyUserAttributes(UserProfile definition, Profile userProfile) {

        List<UserAttribute> userAttributes = new ArrayList<>(definition.getAttributes().values());

        for(Iterator<Attribute> iter = userProfile.getAttributes().iterator();iter.hasNext();) {

            Attribute attribute = iter.next();

            UserAttribute attributeDefinition = definition.getAttributes().get(attribute.getName());

            if (attributeDefinition == null) {
                if(isManaged(attribute)) {

                    iter.remove();
                }
            } else {

                if(isManaged(attribute)) {

                    applyDefinition(attributeDefinition, attribute);
                }

                userAttributes.remove(attributeDefinition);
            }
        }

        // New User Attributes
        for (UserAttribute attributeDefinition:userAttributes) {

            Attribute attribute = new Attribute();
            applyDefinition(attributeDefinition, attribute);
            userProfile.getAttributes().add(attribute);
        }
    }

    private void applyUserAttributeGroups(UserProfile definition, Profile userProfile) {

        List<UserAttributeGroup> userAttributeGroups = new ArrayList<>(definition.getGroups().values());

        for(Iterator<AttributeGroup> iter = userProfile.getGroups().iterator(); iter.hasNext();) {

            AttributeGroup attributeGroup = iter.next();

            UserAttributeGroup attributeGroupDefinition = definition.getGroups().get(attributeGroup.getName());

            if (attributeGroupDefinition == null) {

                iter.remove();
            } else {

                applyDefinition(attributeGroupDefinition, attributeGroup);

                userAttributeGroups.remove(attributeGroupDefinition);
            }
        }

        // New User Attributes
        for (UserAttributeGroup attributeGroupDefinition:userAttributeGroups) {

            AttributeGroup attributeGroup = new AttributeGroup();
            applyDefinition(attributeGroupDefinition, attributeGroup);
            userProfile.getGroups().add(attributeGroup);
        }
    }

    private void applyDefinition(UserAttribute definition, Attribute resource) {

        resource.setName(definition.getName());
        resource.setDisplayName(definition.getDisplayName());
        resource.setGroup(definition.getGroup());
        resource.setMultivalued(Boolean.TRUE.equals(definition.getMultivalued()));
        resource.setValidations(definition.getValidations());
        Attribute.Permissions permissions = new Attribute.Permissions();
        if (definition.getPermissions() != null) {
            permissions.setView(definition.getPermissions().getView());
            permissions.setEdit(definition.getPermissions().getEdit());
        }
        resource.setPermissions(permissions);
        Attribute.Required required = new Attribute.Required();
        if (definition.getRequired() != null) {
            required.setRoles(definition.getRequired().getRoles());
            required.setScopes(definition.getRequired().getScopes());
        }
        resource.setRequired(required);
    }

    private void applyDefinition(UserAttributeGroup definition, AttributeGroup resource) {

        resource.setName(definition.getName());
        resource.setDisplayHeader(definition.getDisplayHeader());
        resource.setDisplayDescription(definition.getDisplayDescription());
    }

}
