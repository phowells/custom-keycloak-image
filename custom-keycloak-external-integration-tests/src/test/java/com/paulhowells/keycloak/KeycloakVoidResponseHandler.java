package com.paulhowells.keycloak;

import org.apache.http.HttpResponse;

public class KeycloakVoidResponseHandler extends KeycloakAbstractResponseHandler<KeycloakVoidResponse> {

    protected KeycloakVoidResponse getResponseInstance() {

        return new KeycloakVoidResponse();
    }

    protected void setResponseBody(KeycloakAbstractResponse result, HttpResponse httpResponse) {

        // do nothing
    }
}
