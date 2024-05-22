package com.paulhowells.keycloak;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
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

    private static final ObjectMapper xmlMapper = new XmlMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static final String clientId = "admin-cli";

    private final String keycloakUrl;
    private final String realmName;
    private final String username;
    private final String password;
    private final CloseableHttpClient httpClient;

    private Map<String, Object> accessTokenResponse;

    public KeycloakRestApi(
        String keycloakUrl,
        String realmName,
        String username,
        String password
    ) {

        this.keycloakUrl = keycloakUrl;
        this.realmName = realmName;
        this.username = username;
        this.password = password;

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

    public KeycloakRestApi(
            String keycloakUrl,
            String realmName
    ) {
        this(keycloakUrl, realmName, null, null);
    }

    public String getAccessToken() {
        logger.debug("<getAccessToken");

        String result;

        if (this.accessTokenResponse == null) {

            try {

                String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realmName);

                final HttpPost request = new HttpPost(tokenUrl);
                request.addHeader("Content-Type", "application/x-www-form-urlencoded");
                request.addHeader("Accept", "application/json");
                final List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("grant_type", "password"));
                params.add(new BasicNameValuePair("username", username));
                params.add(new BasicNameValuePair("password", password));
                params.add(new BasicNameValuePair("client_id", clientId));
                request.setEntity(new UrlEncodedFormEntity(params));

                KeycloakMapResponse response = httpClient.execute(request, new KeycloakMapResponseHandler());

                this.accessTokenResponse = response.body;

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        result = (String) this.accessTokenResponse.get("access_token");

        logger.debug(">getAccessToken " + result);
        return result;
    }

    public String getDirectGrantSaml2AccessToken(
            String username,
            String saml2ResponseXml,
            String clientId,
            String clientSecret
    ) {
        logger.debug("<getDirectGrantSaml2AccessToken");

        String result;

        try {

            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realmName);

            final HttpPost request = new HttpPost(tokenUrl);
            request.addHeader("Content-Type", "application/x-www-form-urlencoded");
            request.addHeader("Accept", "application/json");
            final List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("grant_type", "password"));
            params.add(new BasicNameValuePair("username", username));
            params.add(new BasicNameValuePair("saml2_response", saml2ResponseXml));
            params.add(new BasicNameValuePair("client_id", clientId));
            params.add(new BasicNameValuePair("client_secret", clientSecret));
            request.setEntity(new UrlEncodedFormEntity(params));

            KeycloakMapResponse response = httpClient.execute(request, new KeycloakMapResponseHandler());

            result = (String) response.body.get("access_token");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getDirectGrantSaml2AccessToken " + result);
        return result;
    }

    public String getServiceAccountClientAccessToken(
            String clientId,
            String clientSecret
    ) {
        logger.debug("<getServiceAccountClientAccessToken");

        String result;

        try {

            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realmName);

            final HttpPost request = new HttpPost(tokenUrl);
            request.addHeader("Content-Type", "application/x-www-form-urlencoded");
            request.addHeader("Accept", "application/json");
            final List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("grant_type", "client_credentials"));
            params.add(new BasicNameValuePair("client_id", clientId));
            params.add(new BasicNameValuePair("client_secret", clientSecret));
            request.setEntity(new UrlEncodedFormEntity(params));

            KeycloakMapResponse response = httpClient.execute(request, new KeycloakMapResponseHandler());

            result = (String) response.body.get("access_token");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getServiceAccountClientAccessToken " + result);
        return result;
    }

    public KeycloakMapResponse introspectToken(
            String token,
            String clientId,
            String clientSecret
    ) {
        logger.debug("<introspectToken");

        KeycloakMapResponse result;

        try {

            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token/introspect", keycloakUrl, realmName);
            String credentials = String.format("%s:%s", clientId, clientSecret);
            String base64Credentials = Base64.getEncoder().encodeToString(credentials.getBytes());;

            final HttpPost request = new HttpPost(tokenUrl);
            request.addHeader("Content-Type", "application/x-www-form-urlencoded");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("Basic %s", base64Credentials));
            final List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("token", token));
            request.setEntity(new UrlEncodedFormEntity(params));

            result = httpClient.execute(request, new KeycloakMapResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">introspectToken " + result);
        return result;
    }

    public KeycloakVoidResponse createRealm(
            String realmName,
            boolean enabledInd
    ) {
        logger.debug("<createRealm");
        KeycloakVoidResponse result;

        try {

            String url = String.format("%s/admin/realms", keycloakUrl);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            // {"realm":"newRealm","enabled":true}
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("realm", realmName);
            requestMap.put("enabled", enabledInd);

            String json = mapper.writeValueAsString(requestMap);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">createRealm " + result);
        return result;
    }

    public KeycloakMapResponse getRealmByName(
            String realmName
    ) {
        logger.debug("<getRealmByName");
        KeycloakMapResponse result;

        try {

            String url = String.format("%s/admin/realms/%s", keycloakUrl, realmName);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            result =  httpClient.execute(request, new KeycloakMapResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getRealmByName " + result);
        return result;
    }

    public KeycloakVoidResponse updateRealm(
            String realmName,
            Map<String, Object> realmMap
    ) {
        logger.debug("<updateRealm");
        KeycloakVoidResponse result;

        try {

            // PUT https://custom-keycloak.192.168.105.3.nip.io/admin/realms/custom_80d29563-6e31-4ed0-b403-d0a74f108587
            String url = String.format("%s/admin/realms/%s", keycloakUrl, realmName);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(realmMap);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">updateRealm " + result);
        return result;
    }

    public KeycloakVoidResponse deleteRealm(
            String realmName
    ) {
        logger.debug("<deleteRealm");
        KeycloakVoidResponse result;

        try {

            String url = String.format("%s/admin/realms/%s", keycloakUrl, realmName);

            final HttpDelete request = new HttpDelete(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">deleteRealm " + result);
        return result;
    }

    public KeycloakVoidResponse createSamlClient(
            String realmName,
            SamlEntityDescriptor samlServiceProviderDescriptor
    ) {
        logger.debug("<createSamlClient");
        KeycloakVoidResponse result;

        try {

            // POST https://custom-keycloak.192.168.105.3.nip.io/admin/realms/saml_0e6db644-21f2-4094-a136-a2c082e79dd7/clients
            String url = String.format("%s/admin/realms/%s/clients", keycloakUrl, realmName);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            /*
            {
              "clientId": "https://custom-keycloak.192.168.105.3.nip.io/realms/custom_80d29563-6e31-4ed0-b403-d0a74f108587",
              "protocol": "saml",
              "fullScopeAllowed": true,
              "protocolMappers": [],
              "name": "",
              "description": "",
              "alwaysDisplayInConsole": false,
              "redirectUris": [
                "https://custom-keycloak.192.168.105.3.nip.io/realms/custom_80d29563-6e31-4ed0-b403-d0a74f108587/broker/custom_80d29563-6e31-4ed0-b403-d0a74f108587_saml_0e6db644-21f2-4094-a136-a2c082e79dd7/endpoint"
              ],
              "attributes": {
                "saml.signing.certificate": "MIIC5TCCAc0CBgGOY31/ZDANBgkqhkiG9w0BAQsFADA2MTQwMgYDVQQDDCtjdXN0b21fODBkMjk1NjMtNmUzMS00ZWQwLWI0MDMtZDBhNzRmMTA4NTg3MB4XDTI0MDMyMjAwMDczMloXDTM0MDMyMjAwMDkxMlowNjE0MDIGA1UEAwwrY3VzdG9tXzgwZDI5NTYzLTZlMzEtNGVkMC1iNDAzLWQwYTc0ZjEwODU4NzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKfWxQ1OCQs7msr6JkPqYI3vOtmQKnhY6ilMp/AWiidvmFlrKk9lXXT8rFdySUz7y+HUswCBsdY8E4HPzIAwMlo9p4A2hI0qv2idC12wDxjcjjlA91Zuv3YbgBqybNF03RNAWy2705qBjN7wRp1k5wqA2x4rlvJVgCNiQhkPqpGjluIwNI9oMyoBbrsZqKTLBNcWmKii0Fp0i6AkF7vHQsEfIw9k0vRvZa66oq85248LH4qO5VD5tBhHqRKQB6mTXOwVAz/cW67YpxfhcdsJNz9N2lRRQj9Mfo/530jQ0UBXwMmtc9Wknqo2sefLni0Y20yptoMKvRm/eBGCaJ2SbkECAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAM8vXmAufuEiKTO4bnM3PHotd+FcPA6v5UEXRfiCDW8whPM/MdJ5i1r9u/vBVQZsYe93v/BFGyNlOZ9LEtTrZva3LBn25b0/xExf/qKIw2xSv2a1ZC1iQg57wpghPRyQ66o5xDkO8Ju/95fcGdZ+QZskTZc4KetgRS3Yx6jlGplVgW9h3joaaT62CzfnmUNJVo6Kp/L4BB9R9BagG3eV5zcWP6sEids3LRN8LiCl4W1tvoFMSFoQDoAT+k4gOJbV/4h9R1bbIBN6qawYRHytnpgTlHtNyaOrQQ2GKC8HhCpBwHxeHNSK03R1U/c1axCAHKtEO3TkLq00Vgy18OGdibQ==",
                "saml.signature.algorithm": "RSA_SHA256",
                "saml_single_logout_service_url_post": "https://custom-keycloak.192.168.105.3.nip.io/realms/custom_80d29563-6e31-4ed0-b403-d0a74f108587/broker/custom_80d29563-6e31-4ed0-b403-d0a74f108587_saml_0e6db644-21f2-4094-a136-a2c082e79dd7/endpoint",
                "saml.client.signature": "true",
                "saml.authnstatement": "true",
                "saml_assertion_consumer_url_post": "https://custom-keycloak.192.168.105.3.nip.io/realms/custom_80d29563-6e31-4ed0-b403-d0a74f108587/broker/custom_80d29563-6e31-4ed0-b403-d0a74f108587_saml_0e6db644-21f2-4094-a136-a2c082e79dd7/endpoint",
                "saml_name_id_format": "persistent",
                "saml.server.signature": "true",
                "saml.server.signature.keyinfo.ext": "false",
                "saml.encrypt": false
              }
            }
             */
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("protocol", "saml");
            requestMap.put("clientId", samlServiceProviderDescriptor.entityId);
            requestMap.put("fullScopeAllowed", true);
            requestMap.put("alwaysDisplayInConsole", true);
            List<String> redirectUris = new ArrayList<>();
            redirectUris.add(samlServiceProviderDescriptor.spssoDescriptors.get(0).assertionConsumerServices.get(0).location);
            requestMap.put("redirectUris", redirectUris);
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("saml.signing.certificate", (samlServiceProviderDescriptor.spssoDescriptors.get(0).keyDescriptors.get(0).keyInfos.get(0)).x509Datas.get(0).x509Certificate);
            attributes.put("saml_single_logout_service_url_post", samlServiceProviderDescriptor.spssoDescriptors.get(0).singleLogoutServices.get(0).location);
            attributes.put("saml.client.signature", true);
            attributes.put("saml.authnstatement", true);
            attributes.put("saml_assertion_consumer_url_post", samlServiceProviderDescriptor.spssoDescriptors.get(0).assertionConsumerServices.get(0).location);
            String qualifiedNameFormat = samlServiceProviderDescriptor.spssoDescriptors.get(0).nameIDFormats.get(0);
            int index = qualifiedNameFormat.lastIndexOf(':') + 1;
            String nameFormat = qualifiedNameFormat.substring(index);
            attributes.put("saml_name_id_format", nameFormat);
            attributes.put("saml.server.signature", true);
            attributes.put("saml.server.signature.keyinfo.ext", false);
            attributes.put("saml.encrypt", "false");
            requestMap.put("attributes", attributes);

            /*
<md:EntityDescriptor
	xmlns="urn:oasis:names:tc:SAML:2.0:metadata"
	xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata"
	xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
	xmlns:ds="http://www.w3.org/2000/09/xmldsig#" entityID="https://custom-keycloak.192.168.105.3.nip.io/realms/custom_80d29563-6e31-4ed0-b403-d0a74f108587" ID="ID_dc9487ba-25a0-4dd2-8e02-a6b8f780a59c">
	<md:SPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol" AuthnRequestsSigned="true" WantAssertionsSigned="false">
		<md:KeyDescriptor use="signing">
			<ds:KeyInfo>
				<ds:KeyName>WgjrXgJS8D6-4fnkGvZsDo7mU_AaCfpQAFPhROtjyH4</ds:KeyName>
				<ds:X509Data>
					<ds:X509Certificate>MIIC5TCCAc0CBgGOY31/ZDANBgkqhkiG9w0BAQsFADA2MTQwMgYDVQQDDCtjdXN0b21fODBkMjk1NjMtNmUzMS00ZWQwLWI0MDMtZDBhNzRmMTA4NTg3MB4XDTI0MDMyMjAwMDczMloXDTM0MDMyMjAwMDkxMlowNjE0MDIGA1UEAwwrY3VzdG9tXzgwZDI5NTYzLTZlMzEtNGVkMC1iNDAzLWQwYTc0ZjEwODU4NzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKfWxQ1OCQs7msr6JkPqYI3vOtmQKnhY6ilMp/AWiidvmFlrKk9lXXT8rFdySUz7y+HUswCBsdY8E4HPzIAwMlo9p4A2hI0qv2idC12wDxjcjjlA91Zuv3YbgBqybNF03RNAWy2705qBjN7wRp1k5wqA2x4rlvJVgCNiQhkPqpGjluIwNI9oMyoBbrsZqKTLBNcWmKii0Fp0i6AkF7vHQsEfIw9k0vRvZa66oq85248LH4qO5VD5tBhHqRKQB6mTXOwVAz/cW67YpxfhcdsJNz9N2lRRQj9Mfo/530jQ0UBXwMmtc9Wknqo2sefLni0Y20yptoMKvRm/eBGCaJ2SbkECAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAM8vXmAufuEiKTO4bnM3PHotd+FcPA6v5UEXRfiCDW8whPM/MdJ5i1r9u/vBVQZsYe93v/BFGyNlOZ9LEtTrZva3LBn25b0/xExf/qKIw2xSv2a1ZC1iQg57wpghPRyQ66o5xDkO8Ju/95fcGdZ+QZskTZc4KetgRS3Yx6jlGplVgW9h3joaaT62CzfnmUNJVo6Kp/L4BB9R9BagG3eV5zcWP6sEids3LRN8LiCl4W1tvoFMSFoQDoAT+k4gOJbV/4h9R1bbIBN6qawYRHytnpgTlHtNyaOrQQ2GKC8HhCpBwHxeHNSK03R1U/c1axCAHKtEO3TkLq00Vgy18OGdibQ==</ds:X509Certificate>
				</ds:X509Data>
			</ds:KeyInfo>
		</md:KeyDescriptor>
		<md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="https://custom-keycloak.192.168.105.3.nip.io/realms/custom_80d29563-6e31-4ed0-b403-d0a74f108587/broker/custom_80d29563-6e31-4ed0-b403-d0a74f108587_saml_0e6db644-21f2-4094-a136-a2c082e79dd7/endpoint"/>
		<md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:persistent</md:NameIDFormat>
		<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="https://custom-keycloak.192.168.105.3.nip.io/realms/custom_80d29563-6e31-4ed0-b403-d0a74f108587/broker/custom_80d29563-6e31-4ed0-b403-d0a74f108587_saml_0e6db644-21f2-4094-a136-a2c082e79dd7/endpoint" isDefault="true" index="1"/>
	</md:SPSSODescriptor>
</md:EntityDescriptor>
             */

            String json = mapper.writeValueAsString(requestMap);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">createSamlClient " + result);
        return result;
    }

    public KeycloakVoidResponse createDirectGrantSamlClient(
            String realmName,
            String clientId,
            String name
    ) {
        logger.debug("<createDirectGrantSamlClient");
        KeycloakVoidResponse result;

        try {

            // POST https://custom-keycloak.192.168.105.3.nip.io/admin/realms/saml_0e6db644-21f2-4094-a136-a2c082e79dd7/clients
            String url = String.format("%s/admin/realms/%s/clients", keycloakUrl, realmName);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            /*
                {
                  "protocol": "openid-connect",
                  "clientId": "authenticate_saml",
                  "name": "Authenticate SAML Endpoint",
                  "description": "",
                  "publicClient": false,
                  "authorizationServicesEnabled": false,
                  "serviceAccountsEnabled": false,
                  "implicitFlowEnabled": false,
                  "directAccessGrantsEnabled": true,
                  "standardFlowEnabled": false,
                  "frontchannelLogout": true,
                  "attributes": {
                    "saml_idp_initiated_sso_url_name": "",
                    "oauth2.device.authorization.grant.enabled": false,
                    "oidc.ciba.grant.enabled": false
                  },
                  "alwaysDisplayInConsole": false,
                  "rootUrl": "",
                  "baseUrl": ""
                }
             */
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("protocol", "openid-connect");
            requestMap.put("clientId", clientId);
            requestMap.put("name", name);
            requestMap.put("publicClient", false);
            requestMap.put("authorizationServicesEnabled", false);
            requestMap.put("serviceAccountsEnabled", false);
            requestMap.put("implicitFlowEnabled", false);
            requestMap.put("directAccessGrantsEnabled", true);
            requestMap.put("standardFlowEnabled", false);
            requestMap.put("frontchannelLogout", true);
            requestMap.put("alwaysDisplayInConsole", true);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("saml_idp_initiated_sso_url_name", "");
            attributes.put("oauth2.device.authorization.grant.enabled", false);
            attributes.put("oidc.ciba.grant.enabled", false);
            requestMap.put("attributes", attributes);

            String json = mapper.writeValueAsString(requestMap);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">createDirectGrantSamlClient " + result);
        return result;
    }

    // PUT https://custom-keycloak.192.168.105.3.nip.io/admin/realms/custom_050b94d8-245a-49b1-a1e5-dba1034ebd00/clients/d4ca59db-461b-4f8e-9a6d-ad0237a85b2e
    /*
    {
      "id": "d4ca59db-461b-4f8e-9a6d-ad0237a85b2e",
      "clientId": "authenticate_saml",
      "name": "Authenticate SAML Endpoint",
      "surrogateAuthRequired": false,
      "enabled": true,
      "alwaysDisplayInConsole": true,
      "clientAuthenticatorType": "client-secret",
      "secret": "3wXk7Y7tlQRloZGKRGa8a1cN0vTKfi2u",
      "redirectUris": [],
      "webOrigins": [],
      "notBefore": 0,
      "bearerOnly": false,
      "consentRequired": false,
      "standardFlowEnabled": false,
      "implicitFlowEnabled": false,
      "directAccessGrantsEnabled": true,
      "serviceAccountsEnabled": false,
      "publicClient": false,
      "frontchannelLogout": true,
      "protocol": "openid-connect",
      "attributes": {
        "oidc.ciba.grant.enabled": "false",
        "oauth2.device.authorization.grant.enabled": "false",
        "client.secret.creation.time": "1711403238",
        "backchannel.logout.session.required": "true",
        "backchannel.logout.revoke.offline.tokens": "false",
        "login_theme": "",
        "display.on.consent.screen": false,
        "frontchannel.logout.url": "",
        "backchannel.logout.url": "",
        "logoUri": "",
        "policyUri": "",
        "tosUri": "",
        "access.token.signed.response.alg": "",
        "id.token.signed.response.alg": "",
        "id.token.encrypted.response.alg": "",
        "id.token.encrypted.response.enc": "",
        "user.info.response.signature.alg": "",
        "user.info.encrypted.response.alg": "",
        "user.info.encrypted.response.enc": "",
        "request.object.signature.alg": "",
        "request.object.encryption.alg": "",
        "request.object.encryption.enc": "",
        "request.object.required": "",
        "authorization.signed.response.alg": "",
        "authorization.encrypted.response.alg": "",
        "authorization.encrypted.response.enc": "",
        "exclude.session.state.from.auth.response": "",
        "exclude.issuer.from.auth.response": "",
        "use.refresh.tokens": "true",
        "client_credentials.use_refresh_token": "false",
        "token.response.type.bearer.lower-case": "false",
        "access.token.lifespan": "",
        "client.session.idle.timeout": "",
        "client.session.max.lifespan": "",
        "client.offline.session.idle.timeout": "",
        "client.offline.session.max.lifespan": "",
        "tls.client.certificate.bound.access.tokens": false,
        "pkce.code.challenge.method": "",
        "require.pushed.authorization.requests": "false",
        "client.use.lightweight.access.token.enabled": "false",
        "acr.loa.map": "{}"
      },
      "authenticationFlowBindingOverrides": {
        "browser": "",
        "direct_grant": "c181621c-bf2c-4bad-8c44-d6dd37f7bcc3"
      },
      "fullScopeAllowed": true,
      "nodeReRegistrationTimeout": -1,
      "defaultClientScopes": [
        "web-origins",
        "acr",
        "profile",
        "roles",
        "email"
      ],
      "optionalClientScopes": [
        "address",
        "phone",
        "offline_access",
        "microprofile-jwt"
      ],
      "access": {
        "view": true,
        "configure": true,
        "manage": true
      },
      "description": "",
      "adminUrl": "",
      "rootUrl": "",
      "baseUrl": "",
      "authorizationServicesEnabled": false
    }
     */

    public KeycloakVoidResponse createServiceAccountClient(
            String realmName,
            String clientId,
            String name
    ) {
        logger.debug("<createServiceAccountClient");
        KeycloakVoidResponse result;

        try {

            String url = String.format("%s/admin/realms/%s/clients", keycloakUrl, realmName);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            /*
                {
                  "protocol": "openid-connect",
                  "clientId": "some_client",
                  "name": "Some Client",
                  "description": "",
                  "publicClient": false,
                  "authorizationServicesEnabled": false,
                  "serviceAccountsEnabled": true,
                  "implicitFlowEnabled": false,
                  "directAccessGrantsEnabled": false,
                  "standardFlowEnabled": false,
                  "frontchannelLogout": true,
                  "attributes": {
                    "saml_idp_initiated_sso_url_name": "",
                    "oauth2.device.authorization.grant.enabled": false,
                    "oidc.ciba.grant.enabled": false
                  },
                  "alwaysDisplayInConsole": true,
                  "rootUrl": "",
                  "baseUrl": ""
                }
             */
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("protocol", "openid-connect");
            requestMap.put("clientId", clientId);
            requestMap.put("name", name);
            requestMap.put("publicClient", false);
            requestMap.put("authorizationServicesEnabled", false);
            requestMap.put("serviceAccountsEnabled", true);
            requestMap.put("implicitFlowEnabled", false);
            requestMap.put("directAccessGrantsEnabled", false);
            requestMap.put("standardFlowEnabled", false);
            requestMap.put("frontchannelLogout", true);
            requestMap.put("alwaysDisplayInConsole", true);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("saml_idp_initiated_sso_url_name", "");
            attributes.put("oauth2.device.authorization.grant.enabled", false);
            attributes.put("oidc.ciba.grant.enabled", false);
            requestMap.put("attributes", attributes);

            String json = mapper.writeValueAsString(requestMap);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">createServiceAccountClient " + result);
        return result;
    }

    public KeycloakMapResponse getClientSecret(
            String realmName,
            String clientEntityId
    ) {
        logger.debug("<getClientSecret");
        KeycloakMapResponse result;

        try {

            // https://custom-keycloak.192.168.105.3.nip.io/admin/realms/custom_479e5fea-33de-4892-a29a-0fef28e4a973/clients/e60a74c0-817e-4fd7-88b8-0dec8e1b5385/client-secret
            String url = String.format("%s/admin/realms/%s/clients/%s/client-secret", keycloakUrl, realmName, clientEntityId);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            result =  httpClient.execute(request, new KeycloakMapResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getClientSecret " + result);
        return result;
    }

    public KeycloakVoidResponse createUser(
            String realmName,
            String username,
            String email,
            boolean emailVerifiedInd,
            String firstName,
            String lastName,
            boolean enabledInd,
            Map<String, Object> attributesMap
            // groups
            // attributes
            // requiredActions
    ) {
        logger.debug("<createUser");
        KeycloakVoidResponse result;

        try {

            // https://custom-keycloak.192.168.105.2.nip.io/admin/realms/045e3f7a-0d79-4ed3-823f-92d363f47bb3/users
            String url = String.format("%s/admin/realms/%s/users", keycloakUrl, realmName);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            // {
            //    "attributes": {
            //        "locale": ""
            //    },
            //    "requiredActions": [],
            //    "emailVerified": false,
            //    "username": "realm_admin",
            //    "email": "",
            //    "firstName": "",
            //    "lastName": "",
            //    "groups": [],
            //    "enabled": true
            //}
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("username", username);
            requestMap.put("email", email);
            requestMap.put("emailVerified", emailVerifiedInd);
            requestMap.put("firstName", firstName);
            requestMap.put("lastName", lastName);
            requestMap.put("enabled", enabledInd);
            requestMap.put("attributes", attributesMap);

            String json = mapper.writeValueAsString(requestMap);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">createUser " + result);
        return result;
    }

    public KeycloakMapResponse getUserProfile(
            String realmName
    ) {
        logger.debug("<getUserProfile");
        KeycloakMapResponse result;

        try {

            // https://custom-keycloak.192.168.105.3.nip.io/admin/realms/custom_80d29563-6e31-4ed0-b403-d0a74f108587/users/profile
            String url = String.format("%s/admin/realms/%s/users/profile", keycloakUrl, realmName);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            result =  httpClient.execute(request, new KeycloakMapResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getUserProfile " + result);
        return result;
    }

    public KeycloakVoidResponse updateUserProfile(
            String realmName,
            Map<String, Object> userProfileMap
    ) {
        logger.debug("<updateUserProfile");
        KeycloakVoidResponse result;

        try {

            // PUT https://custom-keycloak.192.168.105.3.nip.io/admin/realms/custom_80d29563-6e31-4ed0-b403-d0a74f108587/users/profile
            String url = String.format("%s/admin/realms/%s/users/profile", keycloakUrl, realmName);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(userProfileMap);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">updateUserProfile " + result);
        return result;
    }

    public KeycloakMapResponse getByLocation(
            String location
    ) {
        logger.debug("<getByLocation");
        KeycloakMapResponse result;

        try {

            final HttpGet request = new HttpGet(location);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            result =  httpClient.execute(request, new KeycloakMapResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getByLocation " + result);
        return result;
    }

    public KeycloakListResponse listClientRolesAvailableToUser(
            String realmName,
            String userEntityId,
            String filter
    ) {
        logger.debug("<listClientRolesForUser");
        KeycloakListResponse result;

        try {

            // https://custom-keycloak.192.168.105.2.nip.io/admin/realms/045e3f7a-0d79-4ed3-823f-92d363f47bb3/ui-ext/available-roles/users/c8f135f4-19d8-47ff-91b1-5503ce9de8cc?first=0&max=11&search=realm-admin
            String url = String.format("%s/admin/realms/%s/ui-ext/available-roles/users/%s?search=%s", keycloakUrl, realmName, userEntityId, filter);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            result =  httpClient.execute(request, new KeycloakListResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">listClientRolesForUser " + result);
        return result;
    }

    public KeycloakVoidResponse assignClientRole(
            String realmName,
            String userEntityId,
            Map<String, Object> clientRole
    ) {
        logger.debug("<assignClientRole");
        KeycloakVoidResponse result;

        try {

            // https://custom-keycloak.192.168.105.2.nip.io/admin/realms/045e3f7a-0d79-4ed3-823f-92d363f47bb3/users/c8f135f4-19d8-47ff-91b1-5503ce9de8cc/role-mappings/clients/02c6af5c-f12d-442e-976a-bc445466a795
            String url = String.format("%s/admin/realms/%s/users/%s/role-mappings/clients/%s", keycloakUrl, realmName, userEntityId, clientRole.get("clientId"));

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            // [{"id":"db0c66a7-edbe-4da9-91ee-e59ff1b50d35","name":"realm-admin","description":"${role_realm-admin}"}]
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("id", clientRole.get("id"));
            requestMap.put("name", clientRole.get("role"));
            requestMap.put("description", clientRole.get("description"));
            List<Map<String, Object>> requestList = new ArrayList<>();
            requestList.add(requestMap);

            String json = mapper.writeValueAsString(requestList);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">assignClientRole " + result);
        return result;
    }

    public KeycloakVoidResponse resetPassword(
            String realmName,
            String userEntityId,
            String password,
            boolean temporaryInd
    ) {
        logger.debug("<resetPassword");
        KeycloakVoidResponse result;

        try {

            // https://custom-keycloak.192.168.105.2.nip.io/admin/realms/88c28d18-cf44-4f73-ae28-20bcbe5181d4/users/98d30bb1-5bed-4a24-9826-87fead7cf577/reset-password
            String url = String.format("%s/admin/realms/%s/users/%s/reset-password", keycloakUrl, realmName, userEntityId);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            // {"temporary":false,"type":"password","value":"password"}
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("type", "password");
            requestMap.put("value", password);
            requestMap.put("temporary", temporaryInd);

            String json = mapper.writeValueAsString(requestMap);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">resetPassword " + result);
        return result;
    }

    public KeycloakListResponse listAuthenticationFlows(
            String realmName
    ) {
        logger.debug("<listAuthenticationFlows");
        KeycloakListResponse result;

        try {

            // https://custom-keycloak.192.168.105.2.nip.io/admin/realms/dc58db94-0f0e-496b-97a0-fc2980dd6166/ui-ext/authentication-management/flows
            String url = String.format("%s/admin/realms/%s/ui-ext/authentication-management/flows", keycloakUrl, realmName);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            result =  httpClient.execute(request, new KeycloakListResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">listAuthenticationFlows " + result);
        return result;
    }

    public KeycloakMapResponse getAuthenticationFlow(
            String realmName,
            String authenticationFlowId
    ) {
        logger.debug("<getAuthenticationFlow");
        KeycloakMapResponse result;

        try {

            String url = String.format("%s/admin/realms/%s/authentication/flows/%s", keycloakUrl, realmName, authenticationFlowId);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            result =  httpClient.execute(request, new KeycloakMapResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getAuthenticationFlow " + result);
        return result;
    }

    public KeycloakVoidResponse createAuthenticationFLow(
            String realmName,
            String alias,
            String description,
            String providerId,
            boolean topLevelInd
    ) {
        logger.debug("<createAuthenticationFLow");
        KeycloakVoidResponse result;

        try {

            String url = String.format("%s/admin/realms/%s/authentication/flows", keycloakUrl, realmName);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("alias", alias);
            requestMap.put("description", description);
            requestMap.put("providerId", providerId);
            requestMap.put("topLevel", topLevelInd);

            String json = mapper.writeValueAsString(requestMap);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">createAuthenticationFLow " + result);
        return result;
    }

    public KeycloakVoidResponse addAuthenticationFlowExecutionExecution(
            String realmName,
            String authenticationFlowAlias,
            String providerId
    ) {
        logger.debug("<addAuthenticationFlowExecutionExecution");
        KeycloakVoidResponse result;

        try {

            String url = String.format("%s/admin/realms/%s/authentication/flows/%s/executions/execution", keycloakUrl, realmName, URLEncoder.encode(authenticationFlowAlias, UTF_8).replace("+", "%20"));

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("provider", providerId);
            requestMap.put("requirement", "ALTERNATE");

            String json = mapper.writeValueAsString(requestMap);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">addAuthenticationFlowExecutionExecution " + result);
        return result;
    }

    public KeycloakVoidResponse addAuthenticationFlowExecutionFlow(
            String realmName,
            String authenticationFlowAlias,
            String alias,
            String description,
            String provider,
            String type
    ) {
        logger.debug("<addAuthenticationFlowExecutionFlow");
        KeycloakVoidResponse result;

        try {

            String url = String.format("%s/admin/realms/%s/authentication/flows/%s/executions/flow", keycloakUrl, realmName, URLEncoder.encode(authenticationFlowAlias, UTF_8).replace("+", "%20"));

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            // {"alias":"External Provider Sub-flow","description":"","provider":"registration-page-form","type":"basic-flow"}
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("alias", alias);
            requestMap.put("description", description);
            requestMap.put("provider", provider);
            requestMap.put("type", type);
            requestMap.put("requirement", "ALTERNATE");

            String json = mapper.writeValueAsString(requestMap);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">addAuthenticationFlowExecutionFlow " + result);
        return result;
    }

    public KeycloakListResponse listAuthenticationFlowExecutions(
            String realmName,
            String authenticationFlowAlias
    ) {
        logger.debug("<listAuthenticationFlowExecutions");
        KeycloakListResponse result;

        try {

            // https://custom-keycloak.192.168.105.2.nip.io/admin/realms/0f30603a-42d4-42af-862d-dc92e908a3ce/authentication/flows/custom%20browser%20flow/executions
            String url = String.format("%s/admin/realms/%s/authentication/flows/%s/executions", keycloakUrl, realmName, URLEncoder.encode(authenticationFlowAlias, UTF_8).replace("+", "%20"));

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            result =  httpClient.execute(request, new KeycloakListResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">listAuthenticationFlowExecutions " + result);
        return result;
    }

    public KeycloakListResponse getClients(
            String realmName
    ) {
        logger.debug("<getClients");
        KeycloakListResponse result;

        try {

            // https://custom-keycloak.192.168.105.4.nip.io/admin/realms/broker_fae6703c-fd6f-477d-85c7-60e3ed31fbf9/clients?first=0&max=11
            String url = String.format("%s/admin/realms/%s/clients", keycloakUrl, realmName);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            result =  httpClient.execute(request, new KeycloakListResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getClients " + result);
        return result;
    }

    public KeycloakVoidResponse updateAuthenticationFlowExecution(
            String realmName,
            String authenticationFlowAlias,
            Map<String, Object> requestMap
    ) {
        logger.debug("<updateAuthenticationFlowExecution");
        KeycloakVoidResponse result;

        try {
            // https://custom-keycloak.192.168.105.2.nip.io/admin/realms/4492058d-b6ba-4200-89ac-0dfa9b9835c5/authentication/flows/custom%20browser%20flow/executions
            String url = String.format("%s/admin/realms/%s/authentication/flows/%s/executions", keycloakUrl, realmName, URLEncoder.encode(authenticationFlowAlias, UTF_8).replace("+", "%20"));

            final HttpPut request = new HttpPut(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            String json = mapper.writeValueAsString(requestMap);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">updateAuthenticationFlowExecution " + result);
        return result;
    }

    public KeycloakVoidResponse configureSamlEmailScope(
            String realmName
    ) {
        logger.debug("<configureSamlEmailScope");
        KeycloakVoidResponse result;

        try {

            // POST https://custom-keycloak.192.168.105.3.nip.io/admin/realms/saml_0737f900-d038-493c-8285-adea2c463092/client-scopes
            String url = String.format("%s/admin/realms/%s/client-scopes", keycloakUrl, realmName);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            // {"name":"other_email","description":"SAML email","type":"optional","protocol":"saml","attributes":{"display.on.consent.screen":"false","consent.screen.text":"","include.in.token.scope":false,"gui.order":""}}
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("name", "saml_email");
            requestMap.put("description", "SAML Email");
            requestMap.put("type", "optional");
            requestMap.put("protocol", "saml");
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("display.on.consent.screen", false);
            attributes.put("consent.screen.text", "");
            attributes.put("include.in.token.scope", false);
            attributes.put("gui.order", "");
            requestMap.put("attributes", attributes);

            String json = mapper.writeValueAsString(requestMap);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">configureSamlEmailScope " + result);
        return result;
    }

    public KeycloakVoidResponse configureSamlEmailScopeMapper(
            String samlEmailScopeLocation
    ) {
        logger.debug("<configureSamlEmailScopeMapper");
        KeycloakVoidResponse result;

        try {

            // POST https://custom-keycloak.192.168.105.3.nip.io/admin/realms/saml_0737f900-d038-493c-8285-adea2c463092/client-scopes/0ba43601-037d-4729-aa7c-5ce6a40d360e/protocol-mappers/models
            String url = String.format("%s/protocol-mappers/models", samlEmailScopeLocation);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            // {"protocol":"saml","protocolMapper":"saml-user-attribute-mapper","name":"Email","config":{"friendly.name":"Email","attribute.name":"Email","attribute.nameformat":"Unspecified","user.attribute":"email"}}
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("protocol", "saml");
            requestMap.put("protocolMapper", "saml-user-attribute-mapper");
            requestMap.put("name", "Email");
            Map<String, Object> config = new HashMap<>();
            config.put("friendly.name", "Email");
            config.put("attribute.name", "Email");
            config.put("attribute.nameformat", "Unspecified");
            config.put("user.attribute", "email");
            requestMap.put("config", config);

            String json = mapper.writeValueAsString(requestMap);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">configureSamlEmailScopeMapper " + result);
        return result;
    }

    public KeycloakVoidResponse addDefaultClientScope(
            String clientLocation,
            String clientScopeId
    ) {
        logger.debug("<addDefaultClientScope");
        KeycloakVoidResponse result;

        try {

            // PUT https://custom-keycloak.192.168.105.3.nip.io/admin/realms/saml_5dc73255-3cd7-4204-a0b4-2e9076cadba0/clients/6439c1f6-a2d7-4ff1-a043-13f9ee927efd/default-client-scopes/053c9214-c0b9-4103-ba87-52a1e602dd8a
            String url = String.format("%s/default-client-scopes/%s", clientLocation, clientScopeId);

            final HttpPut request = new HttpPut(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">addDefaultClientScope " + result);
        return result;
    }
    
    public KeycloakVoidResponse addSamlIdentityProvider(
            String realmName,
            String alias,
            String samlDescriptorUrl,
            SamlEntityDescriptor samlDescriptor,
            boolean hideOnLoginPage,
            String firstBrokerLoginFlowAlias
    ) {
        logger.debug("<addSamlIdentityProvider");
        KeycloakVoidResponse result;

        try {

            // POST https://custom-keycloak.192.168.105.3.nip.io/admin/realms/custom_7823bf64-af46-4937-98a6-de04c5093c6c/identity-provider/instances
            String url = String.format("%s/admin/realms/%s/identity-provider/instances", keycloakUrl, realmName);

            final HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Authorization", String.format("BEARER %s", getAccessToken()));

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("alias", alias);
            requestMap.put("displayName", "");
            requestMap.put("providerId", "saml");
            requestMap.put("firstBrokerLoginFlowAlias", firstBrokerLoginFlowAlias);
            Map<String, Object> configMap = new HashMap<>();
            configMap.put("allowCreate", "false");
            configMap.put("guiOrder", "");
            configMap.put("hideOnLoginPage", hideOnLoginPage);
            String entityId = String.format("%s/realms/%s", keycloakUrl, realmName);
            configMap.put("entityId", entityId);
            configMap.put("idpEntityId", samlDescriptor.entityId);
            configMap.put("singleSignOnServiceUrl", samlDescriptor.idpssoDescriptors.get(0).singleSignOnServices.get(0).location);
            configMap.put("singleLogoutServiceUrl", samlDescriptor.idpssoDescriptors.get(0).singleLogoutServices.get(0).location);
            configMap.put("attributeConsumingServiceName", "");
            configMap.put("backchannelSupported", "false");
            configMap.put("sendIdTokenOnLogout", "true");
            configMap.put("sendClientIdOnLogout", "false");
            configMap.put("nameIDPolicyFormat", samlDescriptor.idpssoDescriptors.get(0).nameIDFormats.get(0));
            configMap.put("principalType", "Subject NameID");
            configMap.put("postBindingResponse", "true");
            configMap.put("postBindingAuthnRequest", "true");
            configMap.put("postBindingLogout", "true");
            configMap.put("wantAuthnRequestsSigned", samlDescriptor.idpssoDescriptors.get(0).wantAuthnRequestsSigned);
            configMap.put("wantAssertionsSigned", "false");
            configMap.put("wantAssertionsEncrypted", "false");
            configMap.put("forceAuthn", "false");
            configMap.put("validateSignature", "true");
            configMap.put("signSpMetadata", "false");
            configMap.put("loginHint", "true");
            configMap.put("allowedClockSkew", 0);
            configMap.put("attributeConsumingServiceIndex", 0);
            configMap.put("signingCertificate", samlDescriptor.idpssoDescriptors.get(0).keyDescriptors.get(0).keyInfos.get(0).x509Datas.get(0).x509Certificate);
            configMap.put("metadataDescriptorUrl", samlDescriptorUrl);
            configMap.put("enabledFromMetadata", "true");
            configMap.put("addExtensionsElementWithKeyInfo", "false");
            configMap.put("signatureAlgorithm", "RSA_SHA256");
            configMap.put("xmlSigKeyInfoKeyNameTransformer", "KEY_ID");
            configMap.put("useMetadataDescriptorUrl", "false");
            requestMap.put("config", configMap);

            String json = mapper.writeValueAsString(requestMap);
            logger.debug("request="+json);

            StringEntity requestEntity = new StringEntity(
                    json,
                    ContentType.APPLICATION_JSON);

            request.setEntity(requestEntity);

            result =  httpClient.execute(request, new KeycloakVoidResponseHandler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">addSamlIdentityProvider " + result);
        return result;
    }

    public String getSamlIdentityProviderDescriptorUrl(
            String realmName
    ) {
        // https://custom-keycloak.192.168.105.2.nip.io/realms/28b7dec4-aaa5-4fde-9905-ec43af6473ba/protocol/saml/descriptor
        return String.format("%s/realms/%s/protocol/saml/descriptor", keycloakUrl, realmName);
    }

    public SamlEntityDescriptor getSamlIdentityProviderDescriptor(
            String realmName
    ) {
        logger.debug("<getSamlIdentityProviderDescriptor");
        SamlEntityDescriptor result;

        try {

            String url = getSamlIdentityProviderDescriptorUrl(realmName);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/xml");

            result =  httpClient.execute(request, httpResponse -> {

                SamlEntityDescriptor result1 = null;

                HttpEntity entity = httpResponse.getEntity();
                if (entity != null && entity.getContentLength() > 0) {
                    result1 = xmlMapper.readValue(entity.getContent(), SamlEntityDescriptor.class);
                }

                return result1;
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getSamlIdentityProviderDescriptor " + result);
        return result;
    }

    public String getSamlServiceProviderDescriptorUrl(
            String realmName,
            String samlIdentityProviderAlias
    ) {
        // https://custom-keycloak.192.168.105.3.nip.io/realms/custom_80d29563-6e31-4ed0-b403-d0a74f108587/broker/custom_80d29563-6e31-4ed0-b403-d0a74f108587_saml_0e6db644-21f2-4094-a136-a2c082e79dd7/endpoint/descriptor
        return String.format("%s/realms/%s/broker/%s/endpoint/descriptor", keycloakUrl, realmName, samlIdentityProviderAlias);
    }

    public SamlEntityDescriptor getSamlServiceProviderDescriptor(
            String realmName,
            String samlIdentityProviderAlias
    ) {
        logger.debug("<getSamlServiceProviderDescriptor");
        SamlEntityDescriptor result;

        try {

            String url = getSamlServiceProviderDescriptorUrl(realmName, samlIdentityProviderAlias);

            final HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/xml");

            result =  httpClient.execute(request, httpResponse -> {

                SamlEntityDescriptor result1 = null;

                HttpEntity entity = httpResponse.getEntity();
                if (entity != null && entity.getContentLength() > 0) {
                    result1 = xmlMapper.readValue(entity.getContent(), SamlEntityDescriptor.class);
                }

                return result1;
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getSamlServiceProviderDescriptor " + result);
        return result;
    }

    public String toString(Object value) {

        String result = "null";

        if (value != null) {

            try {
                result = mapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }
}
