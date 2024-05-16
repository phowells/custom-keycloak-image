package com.paulhowells.keycloak.configurer.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.paulhowells.keycloak.configurer.rest.client.model.BaseModel;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;

import java.io.IOException;

public class KeycloakResourceResponseHandler<T extends BaseModel> extends KeycloakAbstractResponseHandler<KeycloakResourceResponse<T>> {

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final Class<T> clazz;

    public KeycloakResourceResponseHandler(Class<T> clazz) {

        this.clazz = clazz;
    }

    protected KeycloakResourceResponse<T> getResponseInstance() {

        return new KeycloakResourceResponse<T>();
    }

    protected void setResponseBody(KeycloakAbstractResponse result, ClassicHttpResponse httpResponse) throws IOException {

        KeycloakResourceResponse<T> mapResult = (KeycloakResourceResponse<T>) result;

        mapResult.body = getEntityAsResource(httpResponse);
    }

    private T getEntityAsResource(ClassicHttpResponse httpResponse) throws IOException {
        T result = null;

        HttpEntity entity = httpResponse.getEntity();
        if (entity !=null && entity.getContentLength() > 0) {
            result = mapper.readValue(entity.getContent(), clazz);
        }

        return result;
    }
}
