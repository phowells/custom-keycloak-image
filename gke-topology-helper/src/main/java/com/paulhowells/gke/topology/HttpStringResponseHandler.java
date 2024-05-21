package com.paulhowells.gke.topology;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HttpStringResponseHandler extends HttpAbstractResponseHandler<HttpStringResponse> {

    protected HttpStringResponse getResponseInstance() {

        return new HttpStringResponse();
    }

    protected void setResponseBody(HttpAbstractResponse result, HttpResponse httpResponse) throws IOException {

        HttpStringResponse mapResult = (HttpStringResponse) result;

        mapResult.body = getEntityAsString(httpResponse);
    }

    private String getEntityAsString(HttpResponse httpResponse) throws IOException {
        String result = null;

        HttpEntity entity = httpResponse.getEntity();
        if (entity !=null && entity.getContentLength() > 0) {
            result = new String(entity.getContent().readAllBytes(), UTF_8);
        }

        return result;
    }
}
