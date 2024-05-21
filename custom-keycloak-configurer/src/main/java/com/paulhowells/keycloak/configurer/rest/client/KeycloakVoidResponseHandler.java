package com.paulhowells.keycloak.configurer.rest.client;

import org.apache.hc.core5.http.ClassicHttpResponse;

public class KeycloakVoidResponseHandler extends KeycloakAbstractResponseHandler<KeycloakVoidResponse> {

    protected KeycloakVoidResponse getResponseInstance() {

        return new KeycloakVoidResponse();
    }

    protected void setResponseBody(KeycloakAbstractResponse result, ClassicHttpResponse httpResponse) {

        // do nothing
    }
}
