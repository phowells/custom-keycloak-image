package com.paulhowells.gke.topology;

import org.apache.http.HttpResponse;

public class HttpVoidResponseHandler extends HttpAbstractResponseHandler<HttpVoidResponse> {

    protected HttpVoidResponse getResponseInstance() {

        return new HttpVoidResponse();
    }

    protected void setResponseBody(HttpAbstractResponse result, HttpResponse httpResponse) {

        // do nothing
    }
}
