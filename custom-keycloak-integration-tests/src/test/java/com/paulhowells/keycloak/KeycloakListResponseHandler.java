package com.paulhowells.keycloak;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class KeycloakListResponseHandler extends KeycloakAbstractResponseHandler<KeycloakListResponse> {

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);


    protected KeycloakListResponse getResponseInstance() {

        return new KeycloakListResponse();
    }

    protected void setResponseBody(KeycloakAbstractResponse result, HttpResponse httpResponse) throws IOException {

        KeycloakListResponse mapResult = (KeycloakListResponse) result;

        mapResult.body = getEntityAsList(httpResponse);
    }

    private List<Map<String, Object>> getEntityAsList(HttpResponse httpResponse) throws IOException {
        List<Map<String, Object>> result = null;

        HttpEntity entity = httpResponse.getEntity();
        if (entity !=null && entity.getContentLength() > 0) {
            result = mapper.readValue(entity.getContent(), new TypeReference<>() { });
        }

        return result;
    }
}
