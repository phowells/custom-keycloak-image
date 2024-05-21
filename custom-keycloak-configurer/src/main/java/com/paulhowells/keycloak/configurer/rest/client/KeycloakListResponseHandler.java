package com.paulhowells.keycloak.configurer.rest.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.paulhowells.keycloak.configurer.rest.client.model.BaseModel;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class KeycloakListResponseHandler<T extends BaseModel> extends KeycloakAbstractResponseHandler<KeycloakListResponse<T>> {

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private Class<T> clazz;

    public KeycloakListResponseHandler(Class<T> clazz) {

        this.clazz = clazz;
    }


    protected KeycloakListResponse<T> getResponseInstance() {

        return new KeycloakListResponse<T>();
    }

    protected void setResponseBody(KeycloakAbstractResponse result, ClassicHttpResponse httpResponse) throws IOException {

        KeycloakListResponse<T> mapResult = (KeycloakListResponse<T>) result;

        mapResult.body = getEntityAsList(httpResponse);
    }

    private List<T> getEntityAsList(ClassicHttpResponse httpResponse) throws IOException {
        List<T> result = null;

        HttpEntity entity = httpResponse.getEntity();
        if (entity!=null) {

            byte[] bytes = entity.getContent().readAllBytes();

            result = mapper.readValue(bytes, mapper.getTypeFactory().constructCollectionType(List.class, clazz));
            List<Map<String, Object>> raw = mapper.readValue(bytes, new TypeReference<>() {});

            for (int i=0;i<result.size();++i) {

                result.get(i).set_raw(raw.get(i));
            }
        }

        return result;
    }
}
