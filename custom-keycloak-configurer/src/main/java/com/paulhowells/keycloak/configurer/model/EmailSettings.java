package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class EmailSettings extends BaseDefinition {
    private String fromAddress;
    private String fromDisplayName;
    private String replyToAddress;
    private String replyToDisplayName;
    private String envelopeFromAddress;
    private String host;
    private Integer port;
    private Boolean enableSsl;
    private Boolean enableStartTls;
    private Boolean authenticationEnabled;
    private String username;
    private String password;

    @Override
    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = super.isUnchanged(o, parentName, logger);

        if (result){

            EmailSettings other = (EmailSettings) o;

            result = isUnchanged(this.fromAddress, other.fromAddress, parentName, "fromAddress", logger) &
                    isUnchanged(this.fromDisplayName, other.fromDisplayName, parentName, "fromDisplayName", logger) &
                    isUnchanged(this.replyToAddress, other.replyToAddress, parentName, "replyToAddress", logger) &
                    isUnchanged(this.replyToDisplayName, other.replyToDisplayName, parentName, "replyToDisplayName", logger) &
                    isUnchanged(this.envelopeFromAddress, other.envelopeFromAddress, parentName, "envelopeFromAddress", logger) &
                    isUnchanged(this.host, other.host, parentName, "host", logger) &
                    isUnchanged(this.port, other.port, parentName, "port", logger) &
                    isUnchanged(this.enableSsl, other.enableSsl, parentName, "enableSsl", logger) &
                    isUnchanged(this.enableStartTls, other.enableStartTls, parentName, "enableStartTls", logger) &
                    isUnchanged(this.authenticationEnabled, other.authenticationEnabled, parentName, "authenticationEnabled", logger) &
                    isUnchanged(this.username, other.username, parentName, "username", logger) &
                    isUnchanged(this.password, other.password, parentName, "password", logger) ;
        }

        return result;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromDisplayName() {
        return fromDisplayName;
    }

    public void setFromDisplayName(String fromDisplayName) {
        this.fromDisplayName = fromDisplayName;
    }

    public String getReplyToAddress() {
        return replyToAddress;
    }

    public void setReplyToAddress(String replyToAddress) {
        this.replyToAddress = replyToAddress;
    }

    public String getReplyToDisplayName() {
        return replyToDisplayName;
    }

    public void setReplyToDisplayName(String replyToDisplayName) {
        this.replyToDisplayName = replyToDisplayName;
    }

    public String getEnvelopeFromAddress() {
        return envelopeFromAddress;
    }

    public void setEnvelopeFromAddress(String envelopeFromAddress) {
        this.envelopeFromAddress = envelopeFromAddress;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Boolean getEnableSsl() {
        return enableSsl;
    }

    public void setEnableSsl(Boolean enableSsl) {
        this.enableSsl = enableSsl;
    }

    public Boolean getEnableStartTls() {
        return enableStartTls;
    }

    public void setEnableStartTls(Boolean enableStartTls) {
        this.enableStartTls = enableStartTls;
    }

    public Boolean getAuthenticationEnabled() {
        return authenticationEnabled;
    }

    public void setAuthenticationEnabled(Boolean authenticationEnabled) {
        this.authenticationEnabled = authenticationEnabled;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
