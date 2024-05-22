package com.paulhowells.keycloak;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public abstract class KeycloakAbstractResponseHandler<T extends KeycloakAbstractResponse> implements ResponseHandler<T> {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakAbstractResponseHandler.class);

    @Override
    public T handleResponse(HttpResponse httpResponse) throws IOException {
        logger.debug("<handleResponse");

        T result = getResponseInstance();

        int statusCode = httpResponse.getStatusLine().getStatusCode();
        result.statusCode = statusCode;

        if (statusCode < HttpStatus.SC_OK) {
            throw new ClientProtocolException("Unsupported Http Response code " + statusCode);
        } else if (statusCode < HttpStatus.SC_MULTIPLE_CHOICES) {
            // 200 range

            Header location = httpResponse.getFirstHeader("Location");
            if (location != null) {
                result.location = location.getValue();
            }

            setResponseBody(result, httpResponse);

        } else if (statusCode < HttpStatus.SC_BAD_REQUEST) {
            // 300 range

            throw new ClientProtocolException("Unsupported Http Response code "+ statusCode);
        } else if (statusCode < HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            // 400 range

            String body = getEntityAsString(httpResponse);

            throw new ClientProtocolException("Bad Request: " + statusCode + " " + body, new HttpBadRequestException());
        } else {
            // 500 range
            throw new ClientProtocolException("Unsupported Http Response code "+ statusCode);
        }

        logger.debug("<handleResponse "+statusCode+" "+result);
        return result;
    }

    protected abstract T getResponseInstance();

    protected abstract void setResponseBody(KeycloakAbstractResponse result, HttpResponse httpResponse) throws IOException;

    private String getEntityAsString(HttpResponse httpResponse) throws IOException {
        String result = null;

        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {

            result = new BufferedReader(
                    new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
        }

        return result;
    }
}
