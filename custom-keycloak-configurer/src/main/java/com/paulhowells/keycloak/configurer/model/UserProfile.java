package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(converter= UserProfile.PostConstruct.class)
public class UserProfile extends BaseDefinition {

    private Map<String, UserAttribute> attributes = new HashMap<>();
    private Map<String, UserAttributeGroup> groups = new HashMap<>();

    @Override
    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = super.isUnchanged(o, parentName, logger);

        if (result){

            UserProfile other = (UserProfile) o;

            result = isAttributesUnchanged(this.attributes, other.attributes, parentName, "attributes", logger) &
                    isGroupsUnchanged(this.groups, other.groups, parentName, "groups", logger);
        }

        return result;
    }

    protected static boolean isAttributesUnchanged(Map<String, UserAttribute> m1, Map<String, UserAttribute> m2, String parentName, String propertyName, Logger logger) {
        boolean result = true;

        String name;
        if (parentName == null) {
            name = propertyName;
        } else {
            name = String.format("%s.%s", parentName, propertyName);
        }

        Set<String> keys = new HashSet<>();
        keys.addAll(m1.keySet());
        keys.addAll(m2.keySet());

        for (String key:keys) {

            UserAttribute i1 = m1.get(key);
            UserAttribute i2 = m2.get(key);

            result = isUnchanged(i1, i2, String.format("%s[%s]", name, key), false, logger) & result;
        }

        return result;
    }

    protected static boolean isGroupsUnchanged(Map<String, UserAttributeGroup> m1, Map<String, UserAttributeGroup> m2, String parentName, String propertyName, Logger logger) {
        boolean result = true;

        String name;
        if (parentName == null) {
            name = propertyName;
        } else {
            name = String.format("%s.%s", parentName, propertyName);
        }

        Set<String> keys = new HashSet<>();
        keys.addAll(m1.keySet());
        keys.addAll(m2.keySet());

        for (String key:keys) {

            UserAttributeGroup i1 = m1.get(key);
            UserAttributeGroup i2 = m2.get(key);

            result = isUnchanged(i1, i2, String.format("%s[%s]", name, key), false, logger) & result;
        }

        return result;
    }

    public Map<String, UserAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, UserAttribute> attributes) {
        this.attributes = attributes;
    }

    public Map<String, UserAttributeGroup> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, UserAttributeGroup> groups) {
        this.groups = groups;
    }

    static class PostConstruct extends StdConverter<UserProfile, UserProfile> {

        @Override
        public UserProfile convert(UserProfile o) {

            // Set the name on the attributes
            for(String name:o.attributes.keySet()) {

                UserAttribute userAttribute = o.attributes.get(name);

                userAttribute.setName(name);
            }

            // Set the name on the attributeGroups
            for(String name:o.groups.keySet()) {

                UserAttributeGroup attributeGroup = o.groups.get(name);

                attributeGroup.setName(name);
            }

            return o;
        }
    }
}
