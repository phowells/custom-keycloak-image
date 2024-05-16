package com.paulhowells.gke.topology;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class GoogleComputeMetadataApi {

    private static final Logger logger = LoggerFactory.getLogger(GoogleComputeMetadataApi.class);

    private static String computeMetadataUrl = "http://metadata.google.internal/computeMetadata/v1/";

    private final CloseableHttpClient httpClient;

    public GoogleComputeMetadataApi() {

        try {

            SSLContext sslContext = SSLContext.getInstance("SSL");

            // set up a TrustManager that trusts everything
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    logger.debug("getAcceptedIssuers =============");
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs,
                                               String authType) {
                    logger.debug("checkClientTrusted =============");
                }

                public void checkServerTrusted(X509Certificate[] certs,
                                               String authType) {
                    logger.debug("checkServerTrusted =============");
                }
            }}, new SecureRandom());

            this.httpClient = HttpClientBuilder.create()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpStringResponse getInstance() {
        logger.debug("<getInstance");

        HttpStringResponse result;

        try {

            String tokenUrl = String.format("%s/instance/", computeMetadataUrl);

            final HttpPost request = new HttpPost(tokenUrl);
            request.addHeader("Accept", "application/json");
            request.addHeader("Metadata-Flavor", "Google");

            result = httpClient.execute(request, new HttpStringResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getInstance " + result);
        return result;
    }

    public HttpStringResponse getInstanceAttribute(String attribute) {
        logger.debug("<getInstanceAttribute");

        HttpStringResponse result;

        try {

            String tokenUrl = String.format("%s/instance/%s", computeMetadataUrl, attribute);

            final HttpPost request = new HttpPost(tokenUrl);
            request.addHeader("Accept", "application/json");
            request.addHeader("Metadata-Flavor", "Google");

            result = httpClient.execute(request, new HttpStringResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getInstanceAttribute " + result);
        return result;
    }
}
