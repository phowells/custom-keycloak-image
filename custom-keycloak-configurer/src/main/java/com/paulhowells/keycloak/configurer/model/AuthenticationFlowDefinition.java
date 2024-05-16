package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import org.slf4j.Logger;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(converter= AuthenticationFlowDefinition.PostConstruct.class)
public class AuthenticationFlowDefinition extends BaseDefinition {

    private String realmName;
    private String alias;
    private String description;
    private String providerId;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = false)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Execution.Flow.class, name = "flow"),
            @JsonSubTypes.Type(value = Execution.Authenticator.class, name = "authenticator")
    })
    public static abstract class Execution extends BaseDefinition {
        protected String providerId;
        protected String displayName;
        protected String requirement;

        @JsonIgnore
        private String id;
        @JsonIgnore
        private Integer index;

        @JsonDeserialize(converter= Flow.PostConstruct.class)
        public static class Flow extends Execution {
            private String description;
            private List<AuthenticationFlowDefinition.Execution> executions = new ArrayList<>();

            @Override
            public boolean isUnchanged(Object o, String parentName, Logger logger) {
                boolean result = super.isUnchanged(o, parentName, logger);

                if (result){

                    Flow other = (Flow) o;

                    result = isUnchanged(this.description, other.description, parentName, "description", logger) &
                            isUnchanged(this.executions, other.executions, parentName, "executions", logger);
                }

                return result;
            }

            @Override
            public String getType() {
                return "flow";
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            public List<Execution> getExecutions() {
                return executions;
            }

            public void setExecutions(List<Execution> executions) {
                this.executions = executions;
            }

            static class PostConstruct extends StdConverter<Flow, Flow> {

                @Override
                public Flow convert(Flow o) {

                    // Set the index on the executions
                    int i = 0;
                    for (Execution execution: o.executions) {

                        execution.index = i++;
                    }

                    return o;
                }
            }
        }

        public static class Authenticator extends Execution {
            private Map<String, Object> config = new HashMap<>();

            @Override
            public boolean isUnchanged(Object o, String parentName, Logger logger) {
                boolean result = super.isUnchanged(o, parentName, logger);

                if (result){

                    Authenticator other = (Authenticator) o;

                    result = isUnchanged(this.config, other.config, parentName, "config", logger);
                }

                return result;
            }

            @Override
            public String getType() {
                return "authenticator";
            }

            public Map<String, Object> getConfig() {
                return config;
            }

            public void setConfig(Map<String, Object> config) {
                this.config = config;
            }
        }

        @Override
        public String toString() {
            return String.format("%s %s", displayName, providerId);
        }

        @Override
        public boolean isUnchanged(Object o, String parentName, Logger logger) {
            boolean result = super.isUnchanged(o, parentName, logger);

            if (result){

                Execution other = (Execution) o;

                result = isUnchanged(this.getType(), other.getType(), parentName, "type", logger) &
                        isUnchanged(this.providerId, other.providerId, parentName, "providerId", logger) &
                        isUnchanged(this.displayName, other.displayName, parentName, "displayName", logger) &
                        isUnchanged(this.requirement, other.requirement, parentName, "requirement", logger) &
                        isUnchanged(this.index, other.index, parentName, "index", logger);
            }

            return result;
        }

        public abstract String getType();

        // The Jackson marshaller complains if we are missing the setter
        public void setType(String type) {
            // do nothing
        }

        public String getProviderId() {
            return providerId;
        }

        public void setProviderId(String providerId) {
            this.providerId = providerId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getRequirement() {
            return requirement;
        }

        public void setRequirement(String requirement) {
            this.requirement = requirement;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }
    }

    private List<Execution> executions = new ArrayList<>();

    @JsonIgnore
    private String id;

    @Override
    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = super.isUnchanged(o, parentName, logger);

        if (result){

            AuthenticationFlowDefinition other = (AuthenticationFlowDefinition) o;

            result = isUnchanged(this.realmName, other.realmName, parentName, "realmName", logger) &
                    isUnchanged(this.alias, other.alias, parentName, "alias", logger) &
                    isUnchanged(this.description, other.description, parentName, "description", logger) &
                    isUnchanged(this.providerId, other.providerId, parentName, "providerId", logger) &
                    isUnchanged(this.executions, other.executions, parentName, "executions", logger);
        }

        return result;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public List<Execution> getExecutions() {
        return executions;
    }

    public void setExecutions(List<Execution> executions) {
        this.executions = executions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    static class PostConstruct extends StdConverter<AuthenticationFlowDefinition, AuthenticationFlowDefinition> {

        @Override
        public AuthenticationFlowDefinition convert(AuthenticationFlowDefinition o) {

            // Set the index on the executions
            int i = 0;
            for (Execution execution: o.executions) {

                execution.index = i++;
            }

            return o;
        }
    }
}
