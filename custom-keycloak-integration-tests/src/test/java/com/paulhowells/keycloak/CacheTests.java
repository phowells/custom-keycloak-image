package com.paulhowells.keycloak;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CacheTests extends AbstractTest{
    private static final Logger logger = LoggerFactory.getLogger(CacheTests.class);

    private static final String testRealmName = "cache_test_realm";

    private static final String userPassword = "password";

    @Test
    public void test() throws IOException, InterruptedException {
        logger.debug("<test");

        try {
            // Realm name is randomly generated to be unique
            createRealm(testRealmName);

            List<Map<String, Object>> serviceAccountClients = createServiceAccountClients(testRealmName, 500);

            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<Callable<String>> tasks = new ArrayList<>();
            for(Map<String, Object> serviceAccountClient: serviceAccountClients) {

                Callable<String> clientTokenAndIntrospectionTask = () -> {

                    String clientId = (String) serviceAccountClient.get("clientId");
                    String clientSecret = (String) serviceAccountClient.get("clientSecret");

                    String token;
                    try {
                        token = getServiceAccountClientToken(testRealmName, clientId, clientSecret);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    Collections.shuffle(serviceAccountClients);
                    List<Map<String, Object>> introspectingClients = serviceAccountClients.subList(0, (int)(serviceAccountClients.size() * .5) + 1);

                    try {
                        introspectToken(testRealmName, token,  introspectingClients);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    return null;
                };

                tasks.add(clientTokenAndIntrospectionTask);
            }

            executor.invokeAll(tasks);

            executor.shutdown();

            executor.awaitTermination(10, TimeUnit.MINUTES);

        } finally {

            deleteRealm(testRealmName);
        }

        logger.debug(">test");
    }

    private void introspectToken(String realmName, String token, List<Map<String, Object>> serviceAccountClients) throws IOException, InterruptedException {

        try(KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                keycloakUrl,
                realmName
        )) {

            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<Callable<Map<String, Object>>> tasks = new ArrayList<>();
            for(Map<String, Object> serviceAccountClient: serviceAccountClients) {

                Callable<Map<String, Object>> task = () -> {
                    String clientId = (String) serviceAccountClient.get("clientId");
                    String clientSecret = (String) serviceAccountClient.get("clientSecret");

                    KeycloakMapResponse response = keycloakRestApi.introspectToken(
                            token,
                            clientId,
                            clientSecret
                    );
                    assertNotNull(response);
                    assertEquals(200, response.statusCode);
                    assertNotNull(response.body);
                    Map<String, Object> result = response.body;
                    logger.debug(keycloakRestApi.toString(result));

                    return result;
                };

                tasks.add(task);
            }

            executor.invokeAll(tasks);

            executor.shutdown();

            executor.awaitTermination(10, TimeUnit.MINUTES);
        }
    }

    private String getServiceAccountClientToken(String realmName, String clientId, String clientSecret) throws IOException {

        String token;
        try(KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                keycloakUrl,
                realmName
        )) {
            token = keycloakRestApi.getServiceAccountClientAccessToken(
                    clientId,
                    clientSecret
            );
        }

        return token;
    }

    private List<Map<String, Object>> createServiceAccountClients(String realmName, int count) throws IOException {

        List<Map<String, Object>> results = new ArrayList<>(count);

        try(KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                keycloakUrl,
                keycloakMasterRealmName,
                masterAdminUsername,
                masterAdminPassword
        )) {

            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<Callable<Map<String, Object>>> tasks = new ArrayList<>();
            for(int i=0;i<count;++i) {

                Callable<Map<String, Object>> createClientTask = () -> {

                    String clientId = "client" + UUID.randomUUID();
                    String clientEntityId;
                    {
                        KeycloakVoidResponse response = keycloakRestApi.createServiceAccountClient(
                                realmName,
                                clientId,
                                clientId
                        );
                        assertNotNull(response);
                        assertEquals(201, response.statusCode);
                        assertNotNull(response.location);
                        // Keycloak does not return the created User

                        // Extract the entity ID from the location so that we do not need to make a call to fetch the client
                        int index = response.location.lastIndexOf('/') + 1;
                        clientEntityId = response.location.substring(index);
                    }
                    String clientSecret;
                    {
                        KeycloakMapResponse response = keycloakRestApi.getClientSecret(realmName, clientEntityId);
                        assertNotNull(response);
                        assertEquals(200, response.statusCode);
                        assertNotNull(response.body);
                        Map<String, Object> result = response.body;
                        logger.debug(keycloakRestApi.toString(result));

                        clientSecret = (String) result.get("value");
                    }
                    Map<String, Object> result = new HashMap<>();
                    result.put("clientId", clientId);
                    result.put("clientEntityId", clientEntityId);
                    result.put("clientSecret", clientSecret);

                    return result;
                };

                tasks.add(createClientTask);
            }

            List<Future<Map<String, Object>>> futures = executor.invokeAll(tasks);

            executor.shutdown();

            executor.awaitTermination(10, TimeUnit.MINUTES);

            for(Future<Map<String, Object>> future:futures) {

                results.add(future.get());
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return results;
    }
}
