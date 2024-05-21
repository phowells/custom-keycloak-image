package com.paulhowells.keycloak.configurer.rest.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;

import java.io.IOException;
import java.util.Map;

public class KeycloakMapResponseHandler extends KeycloakAbstractResponseHandler<KeycloakMapResponse> {

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);


    protected KeycloakMapResponse getResponseInstance() {

        return new KeycloakMapResponse();
    }

    protected void setResponseBody(KeycloakAbstractResponse result, ClassicHttpResponse httpResponse) throws IOException {

        KeycloakMapResponse mapResult = (KeycloakMapResponse) result;

        mapResult.body = getEntityAsMap(httpResponse);
    }

    private Map<String, Object> getEntityAsMap(ClassicHttpResponse httpResponse) throws IOException {
        Map<String, Object> result = null;

        HttpEntity entity = httpResponse.getEntity();
        if (entity !=null && entity.getContentLength() > 0) {
            result = mapper.readValue(entity.getContent(), new TypeReference<>() { });
        }

        return result;
    }
}
