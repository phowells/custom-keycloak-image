package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class LoginSettings extends BaseDefinition {

    private Boolean registrationAllowed;
    private Boolean registrationEmailAsUsername;
    private Boolean rememberMe;
    private Boolean verifyEmail;
    private Boolean loginWithEmailAllowed;
    private Boolean duplicateEmailsAllowed;
    private Boolean resetPasswordAllowed;
    private Boolean editUsernameAllowed;

    @Override
    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = super.isUnchanged(o, parentName, logger);

        if (result){

            LoginSettings other = (LoginSettings) o;

            result = isUnchanged(this.registrationAllowed, other.registrationAllowed, parentName, "registrationAllowed", logger) &
                    isUnchanged(this.registrationEmailAsUsername, other.registrationEmailAsUsername, parentName, "registrationEmailAsUsername", logger) &
                    isUnchanged(this.rememberMe, other.rememberMe, parentName, "rememberMe", logger) &
                    isUnchanged(this.verifyEmail, other.verifyEmail, parentName, "verifyEmail", logger) &
                    isUnchanged(this.loginWithEmailAllowed, other.loginWithEmailAllowed, parentName, "loginWithEmailAllowed", logger) &
                    isUnchanged(this.duplicateEmailsAllowed, other.duplicateEmailsAllowed, parentName, "duplicateEmailsAllowed", logger) &
                    isUnchanged(this.resetPasswordAllowed, other.resetPasswordAllowed, parentName, "resetPasswordAllowed", logger) &
                    isUnchanged(this.editUsernameAllowed, other.editUsernameAllowed, parentName, "editUsernameAllowed", logger);
        }

        return result;
    }

    public Boolean getRegistrationAllowed() {
        return registrationAllowed;
    }

    public void setRegistrationAllowed(Boolean registrationAllowed) {
        this.registrationAllowed = registrationAllowed;
    }

    public Boolean getRegistrationEmailAsUsername() {
        return registrationEmailAsUsername;
    }

    public void setRegistrationEmailAsUsername(Boolean registrationEmailAsUsername) {
        this.registrationEmailAsUsername = registrationEmailAsUsername;
    }

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public Boolean getVerifyEmail() {
        return verifyEmail;
    }

    public void setVerifyEmail(Boolean verifyEmail) {
        this.verifyEmail = verifyEmail;
    }

    public Boolean getLoginWithEmailAllowed() {
        return loginWithEmailAllowed;
    }

    public void setLoginWithEmailAllowed(Boolean loginWithEmailAllowed) {
        this.loginWithEmailAllowed = loginWithEmailAllowed;
    }

    public Boolean getDuplicateEmailsAllowed() {
        return duplicateEmailsAllowed;
    }

    public void setDuplicateEmailsAllowed(Boolean duplicateEmailsAllowed) {
        this.duplicateEmailsAllowed = duplicateEmailsAllowed;
    }

    public Boolean getResetPasswordAllowed() {
        return resetPasswordAllowed;
    }

    public void setResetPasswordAllowed(Boolean resetPasswordAllowed) {
        this.resetPasswordAllowed = resetPasswordAllowed;
    }

    public Boolean getEditUsernameAllowed() {
        return editUsernameAllowed;
    }

    public void setEditUsernameAllowed(Boolean editUsernameAllowed) {
        this.editUsernameAllowed = editUsernameAllowed;
    }
}
