package com.paulhowells.gke.topology;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.util.Map;

public class HttpMapResponseHandler extends HttpAbstractResponseHandler<HttpMapResponse> {

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);


    protected HttpMapResponse getResponseInstance() {

        return new HttpMapResponse();
    }

    protected void setResponseBody(HttpAbstractResponse result, HttpResponse httpResponse) throws IOException {

        HttpMapResponse mapResult = (HttpMapResponse) result;

        mapResult.body = getEntityAsMap(httpResponse);
    }

    private Map<String, Object> getEntityAsMap(HttpResponse httpResponse) throws IOException {
        Map<String, Object> result = null;

        HttpEntity entity = httpResponse.getEntity();
        if (entity !=null && entity.getContentLength() > 0) {
            result = mapper.readValue(entity.getContent(), new TypeReference<>() { });
        }

        return result;
    }
}
