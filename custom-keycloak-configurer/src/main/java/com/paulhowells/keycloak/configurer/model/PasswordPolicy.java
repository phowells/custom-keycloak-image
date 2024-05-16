package com.paulhowells.keycloak.configurer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PasswordPolicy extends BaseDefinition {
    private Integer minimumLength;
    private Integer digits;
    private Integer specialCharacters;
    private Integer uppercase;
    private Integer lowercase;

    @Override
    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = super.isUnchanged(o, parentName, logger);

        if (result){

            PasswordPolicy other = (PasswordPolicy) o;

            result = isUnchanged(this.digits, other.digits, parentName, "digits", logger) &
                    isUnchanged(this.minimumLength, other.minimumLength, parentName, "minimumLength", logger) &
                    isUnchanged(this.specialCharacters, other.specialCharacters, parentName, "specialCharacters", logger) &
                    isUnchanged(this.uppercase, other.uppercase, parentName, "uppercase", logger) &
                    isUnchanged(this.lowercase, other.lowercase, parentName, "lowercase", logger);
        }

        return result;
    }

    public Integer getDigits() {
        return digits;
    }

    public void setDigits(Integer digits) {
        this.digits = digits;
    }

    public Integer getMinimumLength() {
        return minimumLength;
    }

    public void setMinimumLength(Integer minimumLength) {
        this.minimumLength = minimumLength;
    }

    public Integer getSpecialCharacters() {
        return specialCharacters;
    }

    public void setSpecialCharacters(Integer specialCharacters) {
        this.specialCharacters = specialCharacters;
    }

    public Integer getUppercase() {
        return uppercase;
    }

    public void setUppercase(Integer uppercase) {
        this.uppercase = uppercase;
    }

    public Integer getLowercase() {
        return lowercase;
    }

    public void setLowercase(Integer lowercase) {
        this.lowercase = lowercase;
    }
}
