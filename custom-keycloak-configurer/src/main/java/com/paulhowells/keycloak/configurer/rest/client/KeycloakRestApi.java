package com.paulhowells.keycloak.configurer.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.paulhowells.keycloak.configurer.rest.client.model.*;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.Closeable;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class KeycloakRestApi implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakRestApi.class);

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static final String _clientId = "admin-cli";

    private final String keycloakUrl;
    private final String _realmName;
    private final String _username;
    private final String _password;
    private final CloseableHttpClient httpClient;

    private Map<String, Object> accessTokenResponse;

    public KeycloakRestApi(
        String keycloakUrl,
        String realmName,
        String username,
        String password
    ) {

        this.keycloakUrl = keycloakUrl;
        this._realmName = realmName;
        this._username = username;
        this._password = password;

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

            PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                            .setSslContext(sslContext)
                            .setTlsVersions(TLS.V_1_3)
                            .build())
                    .setDefaultSocketConfig(SocketConfig.custom()
                            .setSoTimeout(Timeout.ofMinutes(1))
                            .build())
                    .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
                    .setConnPoolPolicy(PoolReusePolicy.LIFO)
                    .setDefaultConnectionConfig(ConnectionConfig.custom()
                            .setSocketTimeout(Timeout.ofMinutes(1))
                            .setConnectTimeout(Timeout.ofMinutes(1))
                            .setTimeToLive(TimeValue.ofMinutes(10))
                            .build())
                    .build();

            this.httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setCookieSpec(StandardCookieSpec.STRICT)
                            .build())
                    .build();

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private String getAccessToken() {
        logger.debug("<getAccessToken");
        String result;

        boolean requestToken = true;

        if (this.accessTokenResponse != null) {

            Long expiresAt = (Long) this.accessTokenResponse.get("expires_at");

            if (expiresAt < System.currentTimeMillis()) {

                requestToken = false;
            }
        }

        if (requestToken) {

            try {

                String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, _realmName);

                final HttpPost request = new HttpPost(tokenUrl);
                request.addHeader("Content-Type", "application/x-www-form-urlencoded");
                request.addHeader("Accept", "application/json");
                final List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("grant_type", "password"));
                params.add(new BasicNameValuePair("username", _username));
                params.add(new BasicNameValuePair("password", _password));
                params.add(new BasicNameValuePair("client_id", _clientId));
                request.setEntity(new UrlEncodedFormEntity(params));

                KeycloakMapResponse response = httpClient.execute(request, new KeycloakMapResponseHandler());

                this.accessTokenResponse = response.body;
                Integer expiresIn = (Integer) this.accessTokenResponse.get("expires_in");
                logger.debug("expiresIn={}", expiresIn);
                Long expiresAt = (long) (System.currentTimeMillis() + (expiresIn * .9));
                logger.debug("expiresAt={}", expiresAt);
                this.accessTokenResponse.put("expires_at", expiresAt);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        result = (String) this.accessTokenResponse.get("access_token");

        logger.debug(">getAccessToken " + result);
        return result;
    }

    /**
     *  The Keycloak realms endpoint does not support paging so the realms will be returned all at once.
     */
    public List<Realm> getRealms() {
        logger.debug("<getRealms");
        List<Realm> result;

        try {

            String url = String.format("%s/admin/realms", keycloakUrl);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakListResponse<Realm> response =  httpClient.execute(request, new KeycloakListResponseHandler<>(Realm.class));

            result = response.body;

            if (result != null) {
                result.forEach(
                        t -> t.getIdentityProviders().forEach(
                                ip -> ip.setRealm(t.getRealm())
                        )
                );
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getRealms " + result);
        return result;
    }

    public void createRealm(
            Realm realm
    ) {
        logger.debug("<createRealm");

        try {

            String url = String.format("%s/admin/realms", keycloakUrl);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(realm);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">createRealm");
    }

    public Realm getRealmByName(
            String realmName
    ) {
        logger.debug("<getRealmByName");
        Realm result;

        try {

            String url = String.format("%s/admin/realms/%s", keycloakUrl, realmName);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakResourceResponse<Realm> response = httpClient.execute(request, new KeycloakResourceResponseHandler<>(Realm.class));

            result = response.body;

            if (result != null) {

                result.getIdentityProviders().forEach(
                        ip -> ip.setRealm(result.getRealm())
                );
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getRealmByName " + result);
        return result;
    }

    public void updateRealm(
            String realmName,
            Realm realm
    ) {
        logger.debug("<updateRealm");

        try {

            String url = String.format("%s/admin/realms/%s", keycloakUrl, realmName);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(realm);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">updateRealm");
    }

    public void deleteRealm(
            String realmName
    ) {
        logger.debug("<deleteRealm");

        try {

            String url = String.format("%s/admin/realms/%s", keycloakUrl, realmName);

            final HttpDelete request = new HttpDelete(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">deleteRealm" );
    }

    public ClientSecret getClientSecret(
            String realmName,
            String id
    ) {
        logger.debug("<getClientSecret");
        ClientSecret result;

        try {

            String url = String.format("%s/admin/realms/%s/clients/%s/client-secret", keycloakUrl, realmName, id);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakResourceResponse<ClientSecret> response =  httpClient.execute(request, new KeycloakResourceResponseHandler<>(ClientSecret.class));

            result = response.body;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getClientSecret " + result);
        return result;
    }

    public String createClient(
            String realmName,
            Client client
    ) {
        logger.debug("<createClient");
        String result;

        try {

            String url = String.format("%s/admin/realms/%s/clients", keycloakUrl, realmName);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(client);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            KeycloakVoidResponse response = httpClient.execute(request, new KeycloakVoidResponseHandler());

            // Extract the ID from the location header
            result =  extractIdFromLocation(response);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">createClient {}", result);
        return result;
    }

    public List<Client> getClients(
            String realmName,
            int pageOffset,
            int pageSize
    ) {
        logger.debug("<getClients");
        List<Client> result;

        try {

            String url = String.format("%s/admin/realms/%s/clients?first=%s&max=%s", keycloakUrl, realmName, pageOffset, pageSize);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakListResponse<Client> response =  httpClient.execute(request, new KeycloakListResponseHandler<>(Client.class));

            result = response.body;

            if (result !=null) {

                result.forEach(
                        t -> t.setRealm(realmName)
                );
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getClients " + result);
        return result;
    }

    public Client getClient(
            String realmName,
            String id
    ) {
        logger.debug("<getClient");
        Client result;

        try {

            String url = String.format("%s/admin/realms/%s/clients/%s", keycloakUrl, realmName, id);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakResourceResponse<Client> response =  httpClient.execute(request, new KeycloakResourceResponseHandler<>(Client.class));

            result = response.body;
            if (result!=null) {
                result.setRealm(realmName);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getClients " + result);
        return result;
    }

    public void updateClient(
            String realmName,
            String id,
            Client client
    ) {
        logger.debug("<updateClient");

        try {

            String url = String.format("%s/admin/realms/%s/clients/%s", keycloakUrl, realmName, id);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(client);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">updateClient");
    }

    public void deleteClient(
            String realmName,
            String id
    ) {
        logger.debug("<deleteClient");

        try {

            String url = String.format("%s/admin/realms/%s/clients/%s", keycloakUrl, realmName, id);

            final HttpDelete request = new HttpDelete(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">deleteClient");
    }

    public List<IdentityProvider> getIdentityProviders(
            String realmName
    ) {
        logger.debug("<getIdentityProviders");
        List<IdentityProvider> result;

        try {

            String url = String.format("%s/admin/realms/%s/identity-provider/instances", keycloakUrl, realmName);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakListResponse<IdentityProvider> response =  httpClient.execute(request, new KeycloakListResponseHandler<>(IdentityProvider.class));

            result = response.body;

            if (result != null) {

                result.forEach(
                        t -> t.setRealm(realmName)
                );
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getIdentityProviders " + result);
        return result;
    }

    public void addIdentityProvider(
            String realmName,
            IdentityProvider identityProvider
    ) {
        logger.debug("<addIdentityProvider");

        try {

            String url = String.format("%s/admin/realms/%s/identity-provider/instances", keycloakUrl, realmName);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(identityProvider);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">addIdentityProvider");
    }

    public void updateIdentityProvider(
            String realmName,
            String alias,
            IdentityProvider identityProvider
    ) {
        logger.debug("<updateIdentityProvider");

        try {

            String url = String.format("%s/admin/realms/%s/identity-provider/instances/%s", keycloakUrl, realmName, alias);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(identityProvider);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">updateIdentityProvider");
    }

    public void deleteIdentityProvider(
            String realmName,
            String alias
    ) {
        logger.debug("<deleteIdentityProvider");

        try {

            String url = String.format("%s/admin/realms/%s/identity-provider/instances/%s", keycloakUrl, realmName, alias);

            final HttpDelete request = new HttpDelete(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">deleteIdentityProvider");
    }

    // https://keycloak.paulhowells.dev/admin/realms/master/identity-provider/instances/google/mappers
    // {"name":"Hard Coded Admin Grant","config":{"syncMode":"INHERIT","role":"admin"},"identityProviderMapper":"oidc-hardcoded-role-idp-mapper","identityProviderAlias":"google"}
    public String addIdentityProviderMapper(
            String realmName,
            IdentityProviderMapper identityProviderMapper
    ) {
        logger.debug("<addIdentityProviderMapper");
        String result;

        try {

            String url = String.format("%s/admin/realms/%s/identity-provider/instances/%s/mappers", keycloakUrl, realmName, identityProviderMapper.getIdentityProviderAlias());

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(identityProviderMapper);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            KeycloakVoidResponse response = httpClient.execute(request, new KeycloakVoidResponseHandler());

            result = extractIdFromLocation(response);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">addIdentityProviderMapper");
        return result;
    }

    public List<IdentityProviderMapper> getIdentityProviderMappers(
            String realmName,
            String idpAlias
    ) {
        logger.debug("<getIdentityProviderMappers");
        List<IdentityProviderMapper> result;

        try {

            String url = String.format("%s/admin/realms/%s/identity-provider/instances/%s/mappers", keycloakUrl, realmName, idpAlias);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakListResponse<IdentityProviderMapper> response =  httpClient.execute(request, new KeycloakListResponseHandler<>(IdentityProviderMapper.class));

            result = response.body;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getIdentityProviderMappers " + result);
        return result;
    }

    public IdentityProviderMapper getIdentityProviderMapper(
            String realmName,
            String idpAlias,
            String id
    ) {
        logger.debug("<getIdentityProviderMapper");
        IdentityProviderMapper result;
        try {

            String url = String.format("%s/admin/realms/%s/identity-provider/instances/%s/mappers/%s", keycloakUrl, realmName, idpAlias, id);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakResourceResponse<IdentityProviderMapper> response =  httpClient.execute(request, new KeycloakResourceResponseHandler<>(IdentityProviderMapper.class));

            result = response.body;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getIdentityProviderMapper " + result);
        return result;
    }

    public void updateIdentityProviderMapper(
            String realmName,
            String idpAlias,
            String id,
            IdentityProviderMapper identityProviderMapper
    ) {
        logger.debug("<updateIdentityProviderMapper");

        try {

            String url = String.format("%s/admin/realms/%s/identity-provider/instances/%s/mappers/%s", keycloakUrl, realmName, idpAlias, id);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(identityProviderMapper);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">updateIdentityProviderMapper");
    }

    public void deleteIdentityProviderMapper(
            String realmName,
            String idpAlias,
            String id
    ) {
        logger.debug("<deleteIdentityProviderMapper");

        try {

            String url = String.format("%s/admin/realms/%s/identity-provider/instances/%s/mappers/%s", keycloakUrl, realmName, idpAlias, id);

            final HttpDelete request = new HttpDelete(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">deleteIdentityProviderMapper");
    }

    public String createTopLevelFlow(
            String realmName,
            Flow flow
    ) {
        logger.debug("<createTopLevelFlow");
        String result;

        try {

            String url = String.format("%s/admin/realms/%s/authentication/flows", keycloakUrl, realmName);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            // Set the top level to true
            flow.setTopLevel(true);
            String json = mapper.writeValueAsString(flow);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            KeycloakVoidResponse response = httpClient.execute(request, new KeycloakVoidResponseHandler());

            // Extract the ID from the location header
            result =  extractIdFromLocation(response);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">createTopLevelFlow {}", result);
        return result;
    }

    /**
     * Keycloak will only return top-level flows in the list
     * @param realmName the name of the realm
     * @return the list of top-level flows for the realm
     */
    public List<Flow> getTopLevelFlows(
            String realmName
    ) {
        logger.debug("<getTopLevelFlows");
        List<Flow> result;

        try {

            String url = String.format("%s/admin/realms/%s/authentication/flows", keycloakUrl, realmName);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakListResponse<Flow> response =  httpClient.execute(request, new KeycloakListResponseHandler<>(Flow.class));

            result = response.body;

            if (result !=null) {

                result.forEach(
                        t -> {
                            t.setRealm(realmName);
                            t.setLevel(0);
                        }
                );
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getTopLevelFlows " + result);
        return result;
    }

    public Flow getTopLevelFlow(
            String realmName,
            String id
    ) {
        logger.debug("<getTopLevelFlow");
        Flow result;

        try {

            String url = String.format("%s/admin/realms/%s/authentication/flows/%s", keycloakUrl, realmName, id);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakResourceResponse<Flow> response =  httpClient.execute(request, new KeycloakResourceResponseHandler<>(Flow.class));

            result = response.body;
            if (result!=null) {
                result.setRealm(realmName);
                result.setLevel(0);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getTopLevelFlow " + result);
        return result;
    }

    public Flow getSubFlow(
            Flow parentFlow,
            String id
    ) {
        logger.debug("<getSubFlow");
        Flow result;

        try {

            String url = String.format("%s/admin/realms/%s/authentication/flows/%s", keycloakUrl, parentFlow.getRealm(), id);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakResourceResponse<Flow> response =  httpClient.execute(request, new KeycloakResourceResponseHandler<>(Flow.class));

            result = response.body;
            if (result!=null) {
                result.setRealm(parentFlow.getRealm());
                result.setLevel(parentFlow.getLevel() + 1);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getSubFlow " + result);
        return result;
    }

    public void updateTopLevelFlow(
            String realmName,
            String id,
            Flow flow
    ) {
        logger.debug("<updateTopLevelFlow");

        try {

            String url = String.format("%s/admin/realms/%s/authentication/flows/%s", keycloakUrl, realmName, id);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(flow);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">updateTopLevelFlow");
    }

    public void deleteTopLevelFlow(
            String realmName,
            String id
    ) {
        logger.debug("<deleteTopLevelFlow");

        try {

            String url = String.format("%s/admin/realms/%s/authentication/flows/%s", keycloakUrl, realmName, id);

            final HttpDelete request = new HttpDelete(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">deleteTopLevelFlow");
    }

    public String createFlowExecutionAuthenticator(
            Flow parentFlow,
            String providerId,
            String displayName,
            String requirement
    ) {
        logger.debug("<createFlowExecutionAuthenticator");
        String result;

        try {

            String url = String.format("%s/admin/realms/%s/authentication/flows/%s/executions/execution", keycloakUrl, parentFlow.getRealm(), URLEncoder.encode(parentFlow.getAlias(), UTF_8).replace("+", "%20"));

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            Map<String, Object> body = new HashMap<>();
            body.put("provider", providerId);
            body.put("alias", displayName);
            body.put("requirement", requirement);
            String json = mapper.writeValueAsString(body);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            KeycloakVoidResponse response = httpClient.execute(request, new KeycloakVoidResponseHandler());

            // Extract the ID from the location header
            result =  extractIdFromLocation(response);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">createFlowExecutionAuthenticator {}", result);
        return result;
    }

    // {"alias":"Form Sub Flow","description":"this is the description","provider":"registration-page-form","type":"form-flow"}
    public String createFlowExecutionSubFlow(
            Flow parentFlow,
            String providerId,
            String displayName,
            String description,
            String requirement
    ) {
        logger.debug("<createFlowExecutionSubFlow");
        String result;

        try {

            String url = String.format("%s/admin/realms/%s/authentication/flows/%s/executions/flow", keycloakUrl, parentFlow.getRealm(), URLEncoder.encode(parentFlow.getAlias(), UTF_8).replace("+", "%20"));

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            Map<String, Object> body = new HashMap<>();
            body.put("provider", "registration-page-form");
            body.put("type", providerId);
            body.put("alias", displayName);
            body.put("description", description);
            body.put("requirement", requirement);
            String json = mapper.writeValueAsString(body);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            KeycloakVoidResponse response = httpClient.execute(request, new KeycloakVoidResponseHandler());

            // Extract the ID from the location header
            result =  extractIdFromLocation(response);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">createFlowExecutionSubFlow {}", result);
        return result;
    }

    public FlowExecution getFlowExecution(
            String realmName,
            String id
    ) {
        logger.debug("<getFlowExecution");
        FlowExecution result;

        try {

            //                                             /authentication/flows/5b75a88b-6fb3-418a-aa6d-56a36f958769
            String url = String.format("%s/admin/realms/%s/authentication/executions/%s", keycloakUrl, realmName, id);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakResourceResponse<FlowExecution> response = httpClient.execute(request, new KeycloakResourceResponseHandler<>(FlowExecution.class));

            result = response.body;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getFlowExecution");
        return result;
    }

    public void updateFlowExecution(
            Flow parentFlow,
            FlowExecution flowExecution
    ) {
        logger.debug("<updateFlowExecution");

        try {

            String url = String.format("%s/admin/realms/%s/authentication/flows/%s/executions", keycloakUrl, parentFlow.getRealm(), URLEncoder.encode(parentFlow.getAlias(), UTF_8).replace("+", "%20"));

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(flowExecution);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">updateFlowExecution");
    }

    // RaiseFlowExecutionPriority
    // https://keycloak.paulhowells.dev/admin/realms/app-realm/authentication/executions/962b7c27-437e-4f4a-947d-12c97c105ad5/raise-priority
    public void raiseFlowExecutionPriority(
            String realmName,
            String id
    ) {
        logger.debug("<raiseFlowExecutionPriority");

        try {

            String url = String.format("%s/admin/realms/%s/authentication/executions/%s/raise-priority", keycloakUrl, realmName, id);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = "{}";

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">raiseFlowExecutionPriority");
    }

    // LowerFlowExecutionPriority
    // https://keycloak.paulhowells.dev/admin/realms/app-realm/authentication/executions/962b7c27-437e-4f4a-947d-12c97c105ad5/lower-priority
    public void lowerFlowExecutionPriority(
            String realmName,
            String id
    ) {
        logger.debug("<lowerFlowExecutionPriority");

        try {

            String url = String.format("%s/admin/realms/%s/authentication/executions/%s/raise-priority", keycloakUrl, realmName, id);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = "{}";

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">lowerFlowExecutionPriority");
    }

    public void deleteFlowExecution(
            String realmName,
            String id
    ) {
        logger.debug("<deleteFlowExecution");

        try {

            String url = String.format("%s/admin/realms/%s/authentication/executions/%s", keycloakUrl, realmName, id);

            final HttpDelete request = new HttpDelete(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">deleteFlowExecution");
    }

    public List<FlowExecution> getFlowExecutions(
            Flow parentFlow
    ) {
        logger.debug("<getFlowExecutions");
        List<FlowExecution> result;

        try {

            String url = String.format("%s/admin/realms/%s/authentication/flows/%s/executions", keycloakUrl, parentFlow.getRealm(), URLEncoder.encode(parentFlow.getAlias(), UTF_8).replace("+", "%20"));

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakListResponse<FlowExecution> response =  httpClient.execute(request, new KeycloakListResponseHandler<>(FlowExecution.class));

            result = response.body;

            // Only return the child executions of the parent
            if (result != null) {

                result = result.stream().filter(t -> t.getLevel() == 0).toList();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getFlowExecutions " + result);
        return result;
    }

    public Config getConfig(
            String realmName,
            String configId
    ) {
        logger.debug("<getConfig");
        Config result;

        try {

            String url = String.format("%s/admin/realms/%s/authentication/config/%s", keycloakUrl, realmName, configId);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakResourceResponse<Config> response =  httpClient.execute(request, new KeycloakResourceResponseHandler<>(Config.class));

            result = response.body;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getConfig " + result);
        return result;
    }

    public String createConfig(
            String realmName,
            String executionId,
            Config config
    ) {
        logger.debug("<createConfig");
        String result;

        try {

            String url = String.format("%s/admin/realms/%s/authentication/executions/%s/config", keycloakUrl, realmName, executionId);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(config);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            KeycloakVoidResponse response = httpClient.execute(request, new KeycloakVoidResponseHandler());

            // Extract the ID from the location header
            result =  extractIdFromLocation(response);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">createConfig {}", result);
        return result;
    }

    public void updateConfig(
            String realmName,
            String configId,
            Config config
    ) {
        logger.debug("<updateConfig");

        try {

            String url = String.format("%s/admin/realms/%s/authentication/config/%s", keycloakUrl, realmName, configId);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(config);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">updateConfig");
    }

    public void deleteConfig(
            String realmName,
            String configId
    ) {
        logger.debug("<deleteConfig");

        try {

            String url = String.format("%s/admin/realms/%s/authentication/config/%s", keycloakUrl, realmName, configId);

            final HttpDelete request = new HttpDelete(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">deleteConfig");
    }

    public String createRole(
            String realmName,
            Role role
    ) {
        logger.debug("<createRole");
        String result;

        try {

            String url = String.format("%s/admin/realms/%s/roles", keycloakUrl, realmName);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(role);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            KeycloakVoidResponse response = httpClient.execute(request, new KeycloakVoidResponseHandler());

            // Extract the ID from the location header
            result =  extractIdFromLocation(response);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">createRole {}", result);
        return result;
    }
    // https://keycloak.paulhowells.dev/admin/realms/master/ui-ext/available-roles/roles/84c57957-a567-4fee-a332-f7b523d46fdf?first=0&max=11&search=

    public List<Role> getAllRoles(
            String realmName,
            String parentId,
            int pageOffset,
            int pageSize
    ) {
        logger.debug("<getAllRoles");
        List<Role> result;

        try {

            String accessToken = getAccessToken();

            String url = String.format("%s/admin/realms/%s/ui-ext/available-roles/roles/%s?first=%s&max=%s", keycloakUrl, realmName, parentId, pageOffset, pageSize);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", accessToken));

            KeycloakListResponse<Role> response =  httpClient.execute(request, new KeycloakListResponseHandler<>(Role.class));

            result = response.body;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getAllRoles " + result);
        return result;
    }

    public List<Role> getRoles(
            String realmName,
            int pageOffset,
            int pageSize
    ) {
        logger.debug("<getRoles");
        List<Role> result;

        try {

            String accessToken = getAccessToken();

            String url = String.format("%s/admin/realms/%s/roles?first=%s&max=%s", keycloakUrl, realmName, pageOffset, pageSize);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", accessToken));

            KeycloakListResponse<Role> response =  httpClient.execute(request, new KeycloakListResponseHandler<>(Role.class));

            result = response.body;

            if (result != null) {

                // TODO Does this endpoint support the briefRepresentation parameter?
                result = result
                        .stream()
                        .map(t -> {
                                    // The list method returns an incomplete role
                                    // so we need fo fetch each item individually
                                    Role r = getRole(realmName, t.getId(), accessToken);
                                    r.setRealm(realmName);
                                    return r;
                                }
                        ).toList();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getRoles " + result);
        return result;
    }

    public List<Role> getClientRoles(
            String realmName,
            String id,
            int pageOffset,
            int pageSize
    ) {
        logger.debug("<getClientRoles");
        List<Role> result;

        try {

            String accessToken = getAccessToken();

            String url = String.format("%s/admin/realms/%s/clients/%s/roles?first=%s&max=%s&briefRepresentation=false", keycloakUrl, realmName, id, pageOffset, pageSize);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", accessToken));

            KeycloakListResponse<Role> response =  httpClient.execute(request, new KeycloakListResponseHandler<>(Role.class));

            result = response.body;

            if (result != null) {

                result.forEach(t -> t.setRealm(realmName));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getClientRoles " + result);
        return result;
    }

    public List<Role> getChildRoles(
            String realmName,
            String parentId
    ) {
        logger.debug("<getChildRoles");
        List<Role> result;

        try {

            String accessToken = getAccessToken();

            String url = String.format("%s/admin/realms/%s/roles-by-id/%s/composites", keycloakUrl, realmName, parentId);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", accessToken));

            KeycloakListResponse<Role> response =  httpClient.execute(request, new KeycloakListResponseHandler<>(Role.class));

            result = response.body;

            if (result != null) {

                result = result
                        .stream()
                        .map(t -> {
                                    // The list method returns an incomplete role
                                    // so we need fo fetch each item individually
                                    Role r = getRole(realmName, t.getId(), accessToken);
                                    r.setRealm(realmName);
                                    return r;
                                }
                        ).toList();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getChildRoles " + result);
        return result;
    }

    public String addChildRole(
            String realmName,
            String parentId,
            String childId
    ) {
        logger.debug("<addChildRole");
        String result;

        try {

            String url = String.format("%s/admin/realms/%s/roles-by-id/%s/composites", keycloakUrl, realmName, parentId);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            List<Map<String, Object>> payload = new ArrayList<>();
            Map<String, Object> childRole = new HashMap<>();
            childRole.put("id", childId);
            payload.add(childRole);
            String json = mapper.writeValueAsString(payload);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            KeycloakVoidResponse response = httpClient.execute(request, new KeycloakVoidResponseHandler());

            // Extract the ID from the location header
            result =  extractIdFromLocation(response);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">addChildRole {}", result);
        return result;
    }

    public void removeChildRole(
            String realmName,
            String parentId,
            String childId
    ) {
        logger.debug("<removeChildRole");

        try {

            String url = String.format("%s/admin/realms/%s/roles-by-id/%s/composites", keycloakUrl, realmName, parentId);

            final HttpDelete request = new HttpDelete(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            List<Map<String, Object>> payload = new ArrayList<>();
            Map<String, Object> childRole = new HashMap<>();
            childRole.put("id", childId);
            payload.add(childRole);
            String json = mapper.writeValueAsString(payload);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">removeChildRole");
    }

    private Role getRole(
            String realmName,
            String id,
            String accessToken
    ) {
        logger.debug("<getRole");
        Role result;

        try {

            String url = String.format("%s/admin/realms/%s/roles-by-id/%s", keycloakUrl, realmName, id);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", accessToken));

            KeycloakResourceResponse<Role> response =  httpClient.execute(request, new KeycloakResourceResponseHandler<>(Role.class));

            result = response.body;

            if (result!=null) {
                result.setRealm(realmName);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getRoles " + result);
        return result;
    }

    public Role getRole(
            String realmName,
            String id
    ) {
        return getRole(realmName, id, getAccessToken());
    }

    private Role getRoleByName(
            String realmName,
            String roleName
    ) {
        logger.debug("<getRoleByName");
        Role result;

        try {

            String url = String.format("%s/admin/realms/%s/roles/%s", keycloakUrl, realmName, roleName);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakResourceResponse<Role> response =  httpClient.execute(request, new KeycloakResourceResponseHandler<>(Role.class));

            result = response.body;

            if (result!=null) {
                result.setRealm(realmName);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getRoleByName " + result);
        return result;
    }

    public void updateRole(
            String realmName,
            String id,
            Role role
    ) {
        logger.debug("<updateRole");

        try {

            String url = String.format("%s/admin/realms/%s/roles/%s", keycloakUrl, realmName, id);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(role);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">updateRole");
    }

    public void deleteRole(
            String realmName,
            String id
    ) {
        logger.debug("<deleteRole");

        try {

            String url = String.format("%s/admin/realms/%s/roles/%s", keycloakUrl, realmName, id);

            final HttpDelete request = new HttpDelete(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">deleteRole");
    }

    public String createScope(
            String realmName,
            Scope scope
    ) {
        logger.debug("<createScope");
        String result;

        try {

            String url = String.format("%s/admin/realms/%s/client-scopes", keycloakUrl, realmName);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(scope);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            KeycloakVoidResponse response = httpClient.execute(request, new KeycloakVoidResponseHandler());

            // Extract the ID from the location header
            result =  extractIdFromLocation(response);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">createScope {}", result);
        return result;
    }

    public void addDefaultDefaultScope(
            String realmName,
            String id
    ) {
        logger.debug("<addDefaultDefaultScope");

        try {

            String url = String.format("%s/admin/realms/%s/default-default-client-scopes/%s", keycloakUrl, realmName, id);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = "{}";

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">addDefaultDefaultScope");
    }

    public void addDefaultOptionalScope(
            String realmName,
            String id
    ) {
        logger.debug("<addDefaultOptionalScope");

        try {

            String url = String.format("%s/admin/realms/%s/default-optional-client-scopes/%s", keycloakUrl, realmName, id);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = "{}";

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">addDefaultOptionalScope");
    }

    public void addDefaultScope(
            String realmName,
            String clientId,
            String id
    ) {
        logger.debug("<addDefaultScope");

        try {

            String url = String.format("%s/admin/realms/%s/clients/%s/default-client-scopes/%s", keycloakUrl, realmName, clientId, id);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = "{}";

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">addDefaultScope");
    }

    public void addOptionalScope(
            String realmName,
            String clientId,
            String id
    ) {
        logger.debug("<addOptionalScope");

        try {

            String url = String.format("%s/admin/realms/%s/clients/%s/optional-client-scopes/%s", keycloakUrl, realmName, clientId, id);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = "{}";

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">addOptionalScope");
    }

    public List<Scope> getClientScopes(
            String realmName
    ) {
        logger.debug("<getClientScopes");
        List<Scope> result;

        try {

            String accessToken = getAccessToken();

            String url = String.format("%s/admin/realms/%s/client-scopes", keycloakUrl, realmName);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", accessToken));

            KeycloakListResponse<Scope> response =  httpClient.execute(request, new KeycloakListResponseHandler<>(Scope.class));

            result = response.body;

            if (result != null) {

                result.forEach(t -> t.setRealm(realmName));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getClientScopes " + result);
        return result;
    }

    public List<DefaultScope> getDefaultDefaultScopes(
            String realmName
    ) {
        logger.debug("<getDefaultDefaultScopes");
        List<DefaultScope> result;

        try {

            String accessToken = getAccessToken();

            String url = String.format("%s/admin/realms/%s/default-default-client-scopes", keycloakUrl, realmName);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", accessToken));

            KeycloakListResponse<DefaultScope> response =  httpClient.execute(request, new KeycloakListResponseHandler<>(DefaultScope.class));

            result = response.body;

            if (result != null) {

                result.forEach(t -> {t.setRealm(realmName);t.setType("default");});
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getDefaultDefaultScopes " + result);
        return result;
    }

    public List<DefaultScope> getDefaultOptionalScopes(
            String realmName
    ) {
        logger.debug("<getDefaultOptionalScopes");
        List<DefaultScope> result;

        try {

            String accessToken = getAccessToken();

            String url = String.format("%s/admin/realms/%s/default-optional-client-scopes", keycloakUrl, realmName);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", accessToken));

            KeycloakListResponse<DefaultScope> response =  httpClient.execute(request, new KeycloakListResponseHandler<>(DefaultScope.class));

            result = response.body;

            if (result != null) {

                result.forEach(t -> {t.setRealm(realmName);t.setType("optional");});
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getDefaultOptionalScopes " + result);
        return result;
    }

    public List<DefaultScope> getDefaultDefaultScopes(
            String realmName,
            String id
    ) {
        logger.debug("<getDefaultScopes");
        List<DefaultScope> result;

        try {

            String accessToken = getAccessToken();

            String url = String.format("%s/admin/realms/%s/clients/%s/default-client-scopes", keycloakUrl, realmName, id);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", accessToken));

            KeycloakListResponse<DefaultScope> response =  httpClient.execute(request, new KeycloakListResponseHandler<>(DefaultScope.class));

            result = response.body;

            if (result != null) {

                result.forEach(t -> {t.setRealm(realmName);t.setType("default");});
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getDefaultScopes " + result);
        return result;
    }

    public List<DefaultScope> getDefaultOptionalScopes(
            String realmName,
            String id
    ) {
        logger.debug("<getOptionalScopes");
        List<DefaultScope> result;

        try {

            String accessToken = getAccessToken();

            String url = String.format("%s/admin/realms/%s/clients/%s/optional-client-scopes", keycloakUrl, realmName, id);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", accessToken));

            KeycloakListResponse<DefaultScope> response =  httpClient.execute(request, new KeycloakListResponseHandler<>(DefaultScope.class));

            result = response.body;

            if (result != null) {

                result.forEach(t -> {t.setRealm(realmName);t.setType("optional");});
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getOptionalScopes " + result);
        return result;
    }

    private Scope getScope(
            String realmName,
            String id,
            String accessToken
    ) {
        logger.debug("<getScope");
        Scope result;

        try {

            String url = String.format("%s/admin/realms/%s/client-scopes/%s", keycloakUrl, realmName, id);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", accessToken));

            KeycloakResourceResponse<Scope> response =  httpClient.execute(request, new KeycloakResourceResponseHandler<>(Scope.class));

            result = response.body;

            if (result!=null) {
                result.setRealm(realmName);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getScopes " + result);
        return result;
    }

    public Scope getScope(
            String realmName,
            String id
    ) {
        return getScope(realmName, id, getAccessToken());
    }

    public void updateScope(
            String realmName,
            String id,
            Scope scope
    ) {
        logger.debug("<updateScope");

        try {

            String url = String.format("%s/admin/realms/%s/client-scopes/%s", keycloakUrl, realmName, id);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(scope);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">updateScope");
    }

    public void deleteScope(
            String realmName,
            String id
    ) {
        logger.debug("<deleteScope");

        try {

            String url = String.format("%s/admin/realms/%s/client-scopes/%s", keycloakUrl, realmName, id);

            final HttpDelete request = new HttpDelete(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">deleteScope");
    }

    public void deleteDefaultDefaultScope(
            String realmName,
            String id
    ) {
        logger.debug("<deleteDefaultDefaultScope");

        try {

            String url = String.format("%s/admin/realms/%s/default-default-client-scopes/%s", keycloakUrl, realmName, id);

            final HttpDelete request = new HttpDelete(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">deleteDefaultDefaultScope");
    }

    public void deleteDefaultOptionalScope(
            String realmName,
            String id
    ) {
        logger.debug("<deleteDefaultOptionalScope");

        try {

            String url = String.format("%s/admin/realms/%s/default-default-client-scopes/%s", keycloakUrl, realmName, id);

            final HttpDelete request = new HttpDelete(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">deleteDefaultOptionalScope");
    }

    public void deleteDefaultScope(
            String realmName,
            String clientId,
            String id
    ) {
        logger.debug("<deleteDefaultScope");

        try {

            String url = String.format("%s/admin/realms/%s/clients/%s/default-client-scopes/%s", keycloakUrl, realmName, clientId, id);

            final HttpDelete request = new HttpDelete(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">deleteDefaultScope");
    }

    public void deleteOptionalScope(
            String realmName,
            String clientId,
            String id
    ) {
        logger.debug("<deleteOptionalScope");

        try {

            String url = String.format("%s/admin/realms/%s/clients/%s/optional-client-scopes/%s", keycloakUrl, realmName, clientId, id);

            final HttpDelete request = new HttpDelete(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">deleteOptionalScope");
    }

    // Add List
    // https://keycloak.paulhowells.dev/admin/realms/app-realm/client-scopes/5ec7edbd-4b58-46e8-92c2-3aa809599253/protocol-mappers/add-models
    /*
    [
      {
        "name": "realm roles",
        "protocol": "openid-connect",
        "protocolMapper": "oidc-usermodel-realm-role-mapper",
        "consentRequired": false,
        "config": {
          "introspection.token.claim": "true",
          "multivalued": "true",
          "user.attribute": "foo",
          "access.token.claim": "true",
          "claim.name": "realm_access.roles",
          "jsonType.label": "String"
        }
      }
    ]
     */

    // Add
    // https://keycloak.paulhowells.dev/admin/realms/app-realm/client-scopes/5ec7edbd-4b58-46e8-92c2-3aa809599253/protocol-mappers/models
    /*
    {
      "name": "static audience",
      "protocol": "openid-connect",
      "protocolMapper": "oidc-audience-mapper",
      "config": {
        "included.client.audience": "app-api",
        "included.custom.audience": "",
        "id.token.claim": "false",
        "access.token.claim": "true",
        "lightweight.claim": "false",
        "introspection.token.claim": "true"
      }
    }
     */

    public String addScopeMapper(
            String realmName,
            String scopeId,
            ProtocolMapper protocolMapper
    ) {
        logger.debug("<addScopeMapper");
        String result;

        try {

            String url = String.format("%s/admin/realms/%s/client-scopes/%s/protocol-mappers/models", keycloakUrl, realmName, scopeId);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(protocolMapper);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            KeycloakVoidResponse response = httpClient.execute(request, new KeycloakVoidResponseHandler());

            // Extract the ID from the location header
            result =  extractIdFromLocation(response);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">addScopeMapper {}", result);
        return result;
    }

    public List<ProtocolMapper> getScopeMappers(
            String realmName,
            String id
    ) {
        logger.debug("<getScopeMappers");
        List<ProtocolMapper> results;

        try {

            String url = String.format("%s/admin/realms/%s/client-scopes/%s/protocol-mappers/models", keycloakUrl, realmName, id);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakListResponse<ProtocolMapper> response =  httpClient.execute(request, new KeycloakListResponseHandler<>(ProtocolMapper.class));

            results = response.body;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getScopeMappers " + results);
        return results;
    }

    public ProtocolMapper getScopeMapper(
            String realmName,
            String scopeId,
            String id
    ) {
        logger.debug("<getScopeMapper");
        ProtocolMapper result;

        try {

            String url = String.format("%s/admin/realms/%s/client-scopes/%s/protocol-mappers/models/%s", keycloakUrl, realmName, scopeId, id);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakResourceResponse<ProtocolMapper> response =  httpClient.execute(request, new KeycloakResourceResponseHandler<>(ProtocolMapper.class));

            result = response.body;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getScopeMapper " + result);
        return result;
    }

    public void updateScopeMapper(
            String realmName,
            String scopeId,
            String id,
            ProtocolMapper protocolMapper
    ) {
        logger.debug("<addScopeMapper");

        try {

            String url = String.format("%s/admin/realms/%s/client-scopes/%s/protocol-mappers/models/%s", keycloakUrl, realmName, scopeId, id);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(protocolMapper);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">addScopeMapper");
    }

    public void deleteScopeMapper(
            String realmName,
            String scopeId,
            String id
    ) {
        logger.debug("<deleteScopeMapper");

        try {

            String url = String.format("%s/admin/realms/%s/client-scopes/%s/protocol-mappers/models/%s", keycloakUrl, realmName, scopeId, id);

            final HttpDelete request = new HttpDelete(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">deleteScopeMapper");
    }

    public EventsConfig getEventsConfig(
            String realmName
    ) {
        logger.debug("<getEventsConfig");
        EventsConfig result;

        try {

            String url = String.format("%s/admin/realms/%s/events/config", keycloakUrl, realmName);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakResourceResponse<EventsConfig> response = httpClient.execute(request, new KeycloakResourceResponseHandler<>(EventsConfig.class));

            result = response.body;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getEventsConfig " + result);
        return result;
    }

    public void updateEventsConfig(
            String realmName,
            EventsConfig eventsConfig
    ) {
        logger.debug("<updateEventsConfig");

        try {

            String url = String.format("%s/admin/realms/%s/events/config", keycloakUrl, realmName);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(eventsConfig);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">updateEventsConfig");
    }

    public Profile getUserProfile(
            String realmName
    ) {
        logger.debug("<getUserProfile");
        Profile result;

        try {

            String url = String.format("%s/admin/realms/%s/users/profile", keycloakUrl, realmName);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            KeycloakResourceResponse<Profile> response = httpClient.execute(request, new KeycloakResourceResponseHandler<>(Profile.class));

            result = response.body;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getUserProfile " + result);
        return result;
    }

    public void updateUserProfile(
            String realmName,
            Profile userProfile
    ) {
        logger.debug("<updateUserProfile");

        try {

            String url = String.format("%s/admin/realms/%s/users/profile", keycloakUrl, realmName);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(userProfile);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">updateUserProfile");
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }

    private String extractIdFromLocation(KeycloakAbstractResponse response) {

        int startIndex = response.location.lastIndexOf('/') + 1;
        return response.location.substring(startIndex);
    }
}
