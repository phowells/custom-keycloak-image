package com.paulhowells.keycloak;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTests extends AbstractTest{
    private static final Logger logger = LoggerFactory.getLogger(IntegrationTests.class);

    private static final String IDENTITY_PROVIDER_NAME_USER_ATTRIBUTE = "identity-provider-name";
//    private static final String keycloakUrl = "https://custom-keycloak.192.168.105.4.nip.io";
    private static final String realmAdminUsername = "realm.admin";
    private static final String realmAdminPassword = "password";
    private static final String userPassword = "password";

    private static final String BASIC_FLOW_PROVIDER_ID = "basic-flow";
    private static final String SAML_FIRST_BROKER_LOGIN_FLOW_ALIAS = "saml first broker login flow";
    private static final String SAML_IDP_DETECT_EXISTING_USER_PROVIDER_ID = "saml-idp-detect-existing-user";
    private static final String IDP_AUTO_LINK_PROVIDER_ID = "idp-auto-link";
    private static final String SAML2_DIRECT_GRANT_FLOW_ALIAS = "saml2 direct grant";
    private static final String DIRECT_GRANT_VALIDATE_USERNAME_PROVIDER_ID = "direct-grant-validate-username";
    private static final String DIRECT_GRANT_VALIDATE_SAML2_PROVIDER_ID = "direct-grant-validate-saml2";
    public static final String AUTHENTICATE_SAML_CLIENT_ID = "authenticate_saml";
    public static final String IDP_SELECTOR_BROWSER_FLOW_ALIAS = "idp selector browser flow";
    public static final String AUTH_COOKIE_PROVIDER_ID = "auth-cookie";
    public static final String EXTERNAL_PROVIDER_SUB_FLOW_ALIAS = "External Provider Sub-flow";
    public static final String REGISTRATION_PAGE_FORM_PROVIDER_ID = "registration-page-form";
    public static final String AUTH_USERNAME_FORM_PROVIDER_ID = "auth-username-form";
    public static final String USER_IDENTITY_PROVIDER_REDIRECT_PROVIDER_ID = "user-identity-provider-redirect";
    public static final String BASIC_CREDENTIALS_SUB_FLOW_ALIAS = "BASIC Credentials Sub-flow";
    public static final String AUTH_PASSWORD_FORM_PROVIDER_ID = "auth-password-form";
    public static final String REALM_ADMIN_ROLE_NAME = "realm-admin";
    public static final String REALM_MANAGEMENT_CLIENT_ID = "realm-management";

    @Test
    public void testDirectGrantSaml() throws IOException {

        // TODO Automate the setting of the Direct Grant Flow override on the AUTHENTICATE_SAML client.

        String brokerRealmName = "broker_cfff40b5-647c-47b6-9718-64c540a00a40";

        try(KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                keycloakUrl,
                brokerRealmName,
                realmAdminUsername,
                realmAdminPassword
        )) {

            Map<String, Object> client;
            {
                KeycloakListResponse response = keycloakRestApi.getClients(brokerRealmName);
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                List<Map<String, Object>> results = response.body;
                logger.debug(keycloakRestApi.toString(results));

                client = results.stream()
                        .filter(authenticationFlow -> AUTHENTICATE_SAML_CLIENT_ID.equals(authenticationFlow.get("clientId")))
                        .findAny()
                        .orElse(null);
            }
            assertNotNull(client);

            String clientSecret;
            {
                KeycloakMapResponse response = keycloakRestApi.getClientSecret(brokerRealmName, (String) client.get("id"));
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                Map<String, Object> result = response.body;
                logger.debug(keycloakRestApi.toString(result));

                clientSecret = (String) result.get("value");
            }
            assertNotNull(clientSecret);

            keycloakRestApi.getDirectGrantSaml2AccessToken(
            "aewcfubv.zhpvhgqzyiol",
            "PHNhbWxwOlJlc3BvbnNlIHhtbG5zOnNhbWxwPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6cHJvdG9jb2wiIHhtbG5zOnNhbWw9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iIERlc3RpbmF0aW9uPSJodHRwczovL2N1c3RvbS1rZXljbG9hay4xOTIuMTY4LjEwNS4zLm5pcC5pby9yZWFsbXMvY3VzdG9tX2E1M2Y5ZjZjLTM4YTUtNGE4My1hZTk5LWIyNGRhYzRkNjhmMC9icm9rZXIvY3VzdG9tX2E1M2Y5ZjZjLTM4YTUtNGE4My1hZTk5LWIyNGRhYzRkNjhmMF9zYW1sXzVmNDUxMWJkLTU4Y2EtNGZjYy1iOTE3LWRhNDVkMjI2MzI1Mi9lbmRwb2ludCIgSUQ9IklEX2IwMzIxM2Y5LTA4MmItNDI1My1iNjBiLWZiZjYzNzI0ZWFjOCIgSW5SZXNwb25zZVRvPSJJRF9jZTVlYmU3ZC1mZDQyLTQ0YjYtODY3MC02N2M0MzZjYTdmMzkiIElzc3VlSW5zdGFudD0iMjAyNC0wMy0yNVQyMjoyOToyOC4wNjNaIiBWZXJzaW9uPSIyLjAiPjxzYW1sOklzc3Vlcj5odHRwczovL2N1c3RvbS1rZXljbG9hay4xOTIuMTY4LjEwNS4zLm5pcC5pby9yZWFsbXMvc2FtbF81ZjQ1MTFiZC01OGNhLTRmY2MtYjkxNy1kYTQ1ZDIyNjMyNTI8L3NhbWw6SXNzdWVyPjxkc2lnOlNpZ25hdHVyZSB4bWxuczpkc2lnPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjIj48ZHNpZzpTaWduZWRJbmZvPjxkc2lnOkNhbm9uaWNhbGl6YXRpb25NZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz48ZHNpZzpTaWduYXR1cmVNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzA0L3htbGRzaWctbW9yZSNyc2Etc2hhMjU2Ii8+PGRzaWc6UmVmZXJlbmNlIFVSST0iI0lEX2IwMzIxM2Y5LTA4MmItNDI1My1iNjBiLWZiZjYzNzI0ZWFjOCI+PGRzaWc6VHJhbnNmb3Jtcz48ZHNpZzpUcmFuc2Zvcm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjZW52ZWxvcGVkLXNpZ25hdHVyZSIvPjxkc2lnOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIvPjwvZHNpZzpUcmFuc2Zvcm1zPjxkc2lnOkRpZ2VzdE1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMDQveG1sZW5jI3NoYTI1NiIvPjxkc2lnOkRpZ2VzdFZhbHVlPjhvTi9XMkw5K2lkWWhJYldKYUtONGpXU0xKWmdXZXU5bkM5OVJuR1R6Zms9PC9kc2lnOkRpZ2VzdFZhbHVlPjwvZHNpZzpSZWZlcmVuY2U+PC9kc2lnOlNpZ25lZEluZm8+PGRzaWc6U2lnbmF0dXJlVmFsdWU+U2xENG1HUjgwV0FteHh6YXdWWTVpWXVhZS9MNjN2bEMwVHRRM2pOVGZiT2x4QlNxTTFDdVVjcVN3dXFxMFl3eE5UUWh4Z2U4aUdja3RsNkxRTjVpdCtNYmQ0MzNjK05yUzFLbmtjeWdXYU1BcTZ1dVpaVW42K3cyQk53U2dGeWhTdFhhcTJiMEdhU1VNZGw1dG45ZXZMMzBnM01XWWhmMVU1aW1Zd2xOclMvVjlsM3FDZ2xpcnNUUFpQTkQyUFRWaW9Qc05NdENHUVIrYVFaSXY0dElZVk9QbXd1RWliWlFHSGRKT1dpdmNiVVBBdWVsOThNZFpFR1ZDejBRWVdNcmRSUDcyUW0veURLbzdFV01BdnczRkM3dkhxZ2FmbnlVNGtOZmptenVqMWRKci9VaVV5UllsY0lpWFozVDQxOXNKTzc2WG5keFM2ZG9LMDVDR3g3NzhRPT08L2RzaWc6U2lnbmF0dXJlVmFsdWU+PGRzaWc6S2V5SW5mbz48ZHNpZzpLZXlOYW1lPnNaTDVMbFN1Y1FiY1Z3ZEdhVXlpM3VkQ00zWWxMd1g3SUZCT19ramFvSFE8L2RzaWc6S2V5TmFtZT48ZHNpZzpYNTA5RGF0YT48ZHNpZzpYNTA5Q2VydGlmaWNhdGU+TUlJQzRUQ0NBY2tDQmdHT2Q3b1NiREFOQmdrcWhraUc5dzBCQVFzRkFEQTBNVEl3TUFZRFZRUUREQ2x6WVcxc1h6Vm1ORFV4TVdKa0xUVTRZMkV0Tkdaall5MWlPVEUzTFdSaE5EVmtNakkyTXpJMU1qQWVGdzB5TkRBek1qVXlNakkyTURaYUZ3MHpOREF6TWpVeU1qSTNORFphTURReE1qQXdCZ05WQkFNTUtYTmhiV3hmTldZME5URXhZbVF0TlRoallTMDBabU5qTFdJNU1UY3RaR0UwTldReU1qWXpNalV5TUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQ0FROEFNSUlCQ2dLQ0FRRUFxWXlJdWlybE9qeTJkWTlONGtVcEpreitMUkxJcEI0bWxSQ3ZoKzFuc2VPNjZkWDZ2Z1lBdlpjZE1BNmFYL1NwZWt2cWxna3dPVm9PZGR3MnR5TDFjR1hpai8vRGFQeUZyUi9ibHo1RVRCM0FWdXpza00rMy8vcTQ4TzN0VmVLUXpFaFNJdmM5aXJWNmhjU0FLZGVmbEpGT0JGWmFIWm5EV3VoWldINFh5WFdkTGtucmlMNU1LYkhGWS94T2o1cndQU2NuTURucDhFWXBtcmgzWlNGRWZ3OHFkK1JVSThyTjJXK1lVQWVSMDVwOEtQOWUxMThzYStWeTdjNEFzQnZhRVN1VjE3ZTZic1dsT1Exbkh2VFFDN0EyUUI5NStTNEt5WE82b1BEaG1aVTBEeWRIQjl3YnlKRFhEczV3RldkNTZ2S2V0MEk0UVFtdEw1RDdDQVE0N1FJREFRQUJNQTBHQ1NxR1NJYjNEUUVCQ3dVQUE0SUJBUUJQVGswTzkrVGtJdnBicmFMank3azVmUloxOFhVWm1zUElqczh1QXB2TkV5V3gvSm5ucEN5RS9hL1NLcUVaL2M0UnRtWTkwVnJ2RjV2eGw3bnYvZFI2ai9KZjNwWUMvN2lsUFQ5KzIvY1Vhb2pUZjA0b1ZIbitzbi8zRGlnSzhOdnp3OFlVMmRWc2dPTS9KanZXd0l2SVJER3VubGtwNlVlbXRlMlpiZW12VmNRUVUzdGxGd0tqSnRVR1V5K1hVc2dlY21ENzd3MDJpNHZocXdlMVlpWi81M0pFN1FVdDBaVURzZ1dRMWtvQ2UvclBwa0hjZjJ5T0VHU0t2SG4xbDF1S2ZWWTZEVEE2VWUxUWFoVCtSMTQ5b2YwcHIxdWpjZklYMkxyV3pOdmE5WktLdlhVWEZUOVhZMWsraFFHQjZCVWlORE1DZkFRaWtWUlVoMFFkSVM3MDwvZHNpZzpYNTA5Q2VydGlmaWNhdGU+PC9kc2lnOlg1MDlEYXRhPjwvZHNpZzpLZXlJbmZvPjwvZHNpZzpTaWduYXR1cmU+PHNhbWxwOlN0YXR1cz48c2FtbHA6U3RhdHVzQ29kZSBWYWx1ZT0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOnN0YXR1czpTdWNjZXNzIi8+PC9zYW1scDpTdGF0dXM+PHNhbWw6QXNzZXJ0aW9uIHhtbG5zPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXNzZXJ0aW9uIiBJRD0iSURfZWZiN2Q3OTUtNzU2MS00NGJlLWJhNTYtZjBmOWQ5NTFiMjkzIiBJc3N1ZUluc3RhbnQ9IjIwMjQtMDMtMjVUMjI6Mjk6MjguMDU5WiIgVmVyc2lvbj0iMi4wIj48c2FtbDpJc3N1ZXI+aHR0cHM6Ly9jdXN0b20ta2V5Y2xvYWsuMTkyLjE2OC4xMDUuMy5uaXAuaW8vcmVhbG1zL3NhbWxfNWY0NTExYmQtNThjYS00ZmNjLWI5MTctZGE0NWQyMjYzMjUyPC9zYW1sOklzc3Vlcj48c2FtbDpTdWJqZWN0PjxzYW1sOk5hbWVJRCBGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpuYW1laWQtZm9ybWF0OnBlcnNpc3RlbnQiPkctNTAwMWFlZTMtMTQ1Ni00MzRiLTg5YTQtOGE2ZmJhM2UxNzM0PC9zYW1sOk5hbWVJRD48c2FtbDpTdWJqZWN0Q29uZmlybWF0aW9uIE1ldGhvZD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmNtOmJlYXJlciI+PHNhbWw6U3ViamVjdENvbmZpcm1hdGlvbkRhdGEgSW5SZXNwb25zZVRvPSJJRF9jZTVlYmU3ZC1mZDQyLTQ0YjYtODY3MC02N2M0MzZjYTdmMzkiIE5vdE9uT3JBZnRlcj0iMjAyNC0wMy0yNVQyMjozNDoyNi4wNTlaIiBSZWNpcGllbnQ9Imh0dHBzOi8vY3VzdG9tLWtleWNsb2FrLjE5Mi4xNjguMTA1LjMubmlwLmlvL3JlYWxtcy9jdXN0b21fYTUzZjlmNmMtMzhhNS00YTgzLWFlOTktYjI0ZGFjNGQ2OGYwL2Jyb2tlci9jdXN0b21fYTUzZjlmNmMtMzhhNS00YTgzLWFlOTktYjI0ZGFjNGQ2OGYwX3NhbWxfNWY0NTExYmQtNThjYS00ZmNjLWI5MTctZGE0NWQyMjYzMjUyL2VuZHBvaW50Ii8+PC9zYW1sOlN1YmplY3RDb25maXJtYXRpb24+PC9zYW1sOlN1YmplY3Q+PHNhbWw6Q29uZGl0aW9ucyBOb3RCZWZvcmU9IjIwMjQtMDMtMjVUMjI6Mjk6MjYuMDU5WiIgTm90T25PckFmdGVyPSIyMDI0LTAzLTI1VDIyOjMwOjI2LjA1OVoiPjxzYW1sOkF1ZGllbmNlUmVzdHJpY3Rpb24+PHNhbWw6QXVkaWVuY2U+aHR0cHM6Ly9jdXN0b20ta2V5Y2xvYWsuMTkyLjE2OC4xMDUuMy5uaXAuaW8vcmVhbG1zL2N1c3RvbV9hNTNmOWY2Yy0zOGE1LTRhODMtYWU5OS1iMjRkYWM0ZDY4ZjA8L3NhbWw6QXVkaWVuY2U+PC9zYW1sOkF1ZGllbmNlUmVzdHJpY3Rpb24+PC9zYW1sOkNvbmRpdGlvbnM+PHNhbWw6QXV0aG5TdGF0ZW1lbnQgQXV0aG5JbnN0YW50PSIyMDI0LTAzLTI1VDIyOjI5OjI4LjA2M1oiIFNlc3Npb25JbmRleD0iOWUxYTA4Y2QtZTU4Yi00Yzc2LWJjMjEtZjhiZTFjMWNkMjlhOjo2ZWUyOTJlOS1mMzE0LTRhMjYtOGY1Ny1hN2NhZjRhMDI2ZGYiIFNlc3Npb25Ob3RPbk9yQWZ0ZXI9IjIwMjQtMDMtMjZUMDg6Mjk6MjguMDYzWiI+PHNhbWw6QXV0aG5Db250ZXh0PjxzYW1sOkF1dGhuQ29udGV4dENsYXNzUmVmPnVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphYzpjbGFzc2VzOnVuc3BlY2lmaWVkPC9zYW1sOkF1dGhuQ29udGV4dENsYXNzUmVmPjwvc2FtbDpBdXRobkNvbnRleHQ+PC9zYW1sOkF1dGhuU3RhdGVtZW50PjxzYW1sOkF0dHJpYnV0ZVN0YXRlbWVudD48c2FtbDpBdHRyaWJ1dGUgRnJpZW5kbHlOYW1lPSJFbWFpbCIgTmFtZT0iRW1haWwiIE5hbWVGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphdHRybmFtZS1mb3JtYXQ6dW5zcGVjaWZpZWQiPjxzYW1sOkF0dHJpYnV0ZVZhbHVlIHhtbG5zOnhzPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSIgeG1sbnM6eHNpPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYS1pbnN0YW5jZSIgeHNpOnR5cGU9InhzOnN0cmluZyI+YWV3Y2Z1YnYuemhwdmhncXp5aW9sQHNhbWw1ZjQ1MTEuY29tPC9zYW1sOkF0dHJpYnV0ZVZhbHVlPjwvc2FtbDpBdHRyaWJ1dGU+PHNhbWw6QXR0cmlidXRlIE5hbWU9IlJvbGUiIE5hbWVGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphdHRybmFtZS1mb3JtYXQ6YmFzaWMiPjxzYW1sOkF0dHJpYnV0ZVZhbHVlIHhtbG5zOnhzPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSIgeG1sbnM6eHNpPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYS1pbnN0YW5jZSIgeHNpOnR5cGU9InhzOnN0cmluZyI+ZGVmYXVsdC1yb2xlcy1zYW1sXzVmNDUxMWJkLTU4Y2EtNGZjYy1iOTE3LWRhNDVkMjI2MzI1Mjwvc2FtbDpBdHRyaWJ1dGVWYWx1ZT48L3NhbWw6QXR0cmlidXRlPjxzYW1sOkF0dHJpYnV0ZSBOYW1lPSJSb2xlIiBOYW1lRm9ybWF0PSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXR0cm5hbWUtZm9ybWF0OmJhc2ljIj48c2FtbDpBdHRyaWJ1dGVWYWx1ZSB4bWxuczp4cz0iaHR0cDovL3d3dy53My5vcmcvMjAwMS9YTUxTY2hlbWEiIHhtbG5zOnhzaT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS9YTUxTY2hlbWEtaW5zdGFuY2UiIHhzaTp0eXBlPSJ4czpzdHJpbmciPm1hbmFnZS1hY2NvdW50LWxpbmtzPC9zYW1sOkF0dHJpYnV0ZVZhbHVlPjwvc2FtbDpBdHRyaWJ1dGU+PHNhbWw6QXR0cmlidXRlIE5hbWU9IlJvbGUiIE5hbWVGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphdHRybmFtZS1mb3JtYXQ6YmFzaWMiPjxzYW1sOkF0dHJpYnV0ZVZhbHVlIHhtbG5zOnhzPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSIgeG1sbnM6eHNpPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYS1pbnN0YW5jZSIgeHNpOnR5cGU9InhzOnN0cmluZyI+bWFuYWdlLWFjY291bnQ8L3NhbWw6QXR0cmlidXRlVmFsdWU+PC9zYW1sOkF0dHJpYnV0ZT48c2FtbDpBdHRyaWJ1dGUgTmFtZT0iUm9sZSIgTmFtZUZvcm1hdD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmF0dHJuYW1lLWZvcm1hdDpiYXNpYyI+PHNhbWw6QXR0cmlidXRlVmFsdWUgeG1sbnM6eHM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hIiB4bWxuczp4c2k9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hLWluc3RhbmNlIiB4c2k6dHlwZT0ieHM6c3RyaW5nIj5vZmZsaW5lX2FjY2Vzczwvc2FtbDpBdHRyaWJ1dGVWYWx1ZT48L3NhbWw6QXR0cmlidXRlPjxzYW1sOkF0dHJpYnV0ZSBOYW1lPSJSb2xlIiBOYW1lRm9ybWF0PSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXR0cm5hbWUtZm9ybWF0OmJhc2ljIj48c2FtbDpBdHRyaWJ1dGVWYWx1ZSB4bWxuczp4cz0iaHR0cDovL3d3dy53My5vcmcvMjAwMS9YTUxTY2hlbWEiIHhtbG5zOnhzaT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS9YTUxTY2hlbWEtaW5zdGFuY2UiIHhzaTp0eXBlPSJ4czpzdHJpbmciPnVtYV9hdXRob3JpemF0aW9uPC9zYW1sOkF0dHJpYnV0ZVZhbHVlPjwvc2FtbDpBdHRyaWJ1dGU+PHNhbWw6QXR0cmlidXRlIE5hbWU9IlJvbGUiIE5hbWVGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphdHRybmFtZS1mb3JtYXQ6YmFzaWMiPjxzYW1sOkF0dHJpYnV0ZVZhbHVlIHhtbG5zOnhzPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSIgeG1sbnM6eHNpPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYS1pbnN0YW5jZSIgeHNpOnR5cGU9InhzOnN0cmluZyI+dmlldy1wcm9maWxlPC9zYW1sOkF0dHJpYnV0ZVZhbHVlPjwvc2FtbDpBdHRyaWJ1dGU+PC9zYW1sOkF0dHJpYnV0ZVN0YXRlbWVudD48L3NhbWw6QXNzZXJ0aW9uPjwvc2FtbHA6UmVzcG9uc2U+",
                    AUTHENTICATE_SAML_CLIENT_ID,
                    clientSecret
            );
        }
    }

    @Test
    public void test() throws IOException {
        logger.debug("<test");

        // Realm name is randomly generated to be unique
        String brokerRealmName = "broker_"+UUID.randomUUID();
        createRealm(brokerRealmName);

        // Realm Admin username and password will always be the same
        createRealmAdminUser(brokerRealmName);

        configureCustomUserAttributes(brokerRealmName);

        // 1. Check for Cookie (SSO)
        // 2. Get user by username (Required)
        // 3a. Check for identity-provider-name user attribute
        // 4a. Authenticate using identity provider (Required)
        // 3b. Authenticate using password (Required)
        configureCustomBrowserFlow(brokerRealmName);

        // 1. Detect existing user
        // 2. Automatically link user
        configureCustomFirstBrokerLoginFlow(brokerRealmName);

        // 1. Validate Username
        // 2. Validate SAML2 response
        configureSamlDirectGrantFlow(brokerRealmName);

        //Set up a bunch of realms that expose a SAML endpoint and then configure those realms as external SAML IDPs in our custom realm?
        for (int i=0;i<2;++i) {
            List<Map<String, String>> userMaps = createSamlProviderWithUsers(brokerRealmName, 10);

            // Add the users from the SAML Provider to the Broker Keycloak setting the IDP attribute
            addSamlUsersToBroker(brokerRealmName, userMaps);
        }
        // TODO Create a client that is allowed to do Authorization Code logins
        // TODO Log one of the users into Keycloak

        createAuthenticateSamlEndpointClient(brokerRealmName);

        logger.debug(">test");
    }

    private void createAuthenticateSamlEndpointClient(String realmName) throws IOException {

        try(KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                keycloakUrl,
                realmName,
                realmAdminUsername,
                realmAdminPassword
        )) {

            String clientEntityId;
            {
                KeycloakVoidResponse response = keycloakRestApi.createDirectGrantSamlClient(
                        realmName,
                        AUTHENTICATE_SAML_CLIENT_ID,
                        "Authenticate SAML Endpoint"
                );
                assertNotNull(response);
                assertEquals(201, response.statusCode);
                assertNotNull(response.location);
                // Keycloak does not return the created User

                // Extract the entity ID from the location so that we do not need to make a call to fetch the client
                int index = response.location.lastIndexOf('/') + 1;
                clientEntityId = response.location.substring(index);
            }
            {
                KeycloakMapResponse response = keycloakRestApi.getClientSecret(realmName, clientEntityId);
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                Map<String, Object> realm = response.body;
                logger.debug(keycloakRestApi.toString(realm));
            }
        }
    }

    private void configureCustomUserAttributes(String realmName) throws IOException {

        try(KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                keycloakUrl,
                realmName,
                realmAdminUsername,
                realmAdminPassword
        )) {

            Map<String, Object> userProfile;
            {
                KeycloakMapResponse response = keycloakRestApi.getUserProfile(realmName);
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                userProfile = response.body;
                logger.debug(keycloakRestApi.toString(userProfile));
            }
            {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> attributes = (List<Map<String, Object>>) userProfile.get("attributes");
                Map<String, Object> identityProviderNameAttribute = new HashMap<>();
                identityProviderNameAttribute.put("name", IDENTITY_PROVIDER_NAME_USER_ATTRIBUTE);
                // multivalued not supported by Red Hat Keycloak 22
//                identityProviderNameAttribute.put("multivalued", false);
                Map<String, Object> permissions = new HashMap<>();
                List<String> view = new ArrayList<>();
                permissions.put("view", view);
                List<String> edit = new ArrayList<>();
                edit.add("admin");
                permissions.put("edit", edit);
                identityProviderNameAttribute.put("permissions", permissions);
                attributes.add(identityProviderNameAttribute);
                KeycloakVoidResponse response = keycloakRestApi.updateUserProfile(
                        realmName,
                        userProfile
                );
                assertNotNull(response);
                assertEquals(200, response.statusCode);
            }
        }
    }

    private void addSamlUsersToBroker(String brokerRealmName, List<Map<String, String>> userMaps) throws IOException {

        try(KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                keycloakUrl,
                brokerRealmName,
                realmAdminUsername,
                realmAdminPassword
        )) {

            Iterable<Map<String, String>> iterable = () -> userMaps.iterator();
            Stream<Map<String, String>> stream = StreamSupport.stream(iterable.spliterator(), true);
            stream.forEach(user -> {

                String firstName = user.get("firstName");
                String lastName = user.get("lastName");
                String username = user.get("username");
                String email = user.get("email");
                boolean emailVerifiedInd = false;
                boolean enabledInd = true;
                Map<String, Object> attributesMap = new HashMap<>();
                attributesMap.put(IDENTITY_PROVIDER_NAME_USER_ATTRIBUTE, getSamlIdentityProviderAlias(brokerRealmName, user.get("realmName")));

                {
                    KeycloakVoidResponse response = keycloakRestApi.createUser(
                            brokerRealmName,
                            username,
                            email,
                            emailVerifiedInd,
                            firstName,
                            lastName,
                            enabledInd,
                            attributesMap
                    );
                    assertNotNull(response);
                    assertEquals(201, response.statusCode);
                    assertNotNull(response.location);
                    // Keycloak does not return the created User
                }
            });
        }
    }

    private List<Map<String, String>> createSamlProviderWithUsers(String brokerRealmName, int userCount) throws IOException {

        // Realm name is randomly generated to be unique
        String realmName = "saml_idp_"+UUID.randomUUID();
        createRealm(realmName);

        // Realm Admin username and password will always be the same
        createRealmAdminUser(realmName);

        configureRealmAsSamlProvider(brokerRealmName, realmName);

        return createBasicUsers(realmName, userCount);
    }

    private String getSamlIdentityProviderAlias(String realmName, String samlRealmName) {

        return realmName+"_"+samlRealmName;
    }

    private void configureRealmAsSamlProvider(String serviceProviderRealmName, String identityProviderRealmName) throws IOException {

        String samlIdentityProviderAlias = getSamlIdentityProviderAlias(serviceProviderRealmName, identityProviderRealmName);

        try(KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                keycloakUrl,
                serviceProviderRealmName,
                realmAdminUsername,
                realmAdminPassword
        )) {

            // Get the SAML Identity Provider Descriptor from the Identity Provider Realm then use it to create a SAML IDP in the Service Provider realm
            String samlIdentityProviderDescriptorUrl = keycloakRestApi.getSamlIdentityProviderDescriptorUrl(identityProviderRealmName);
            SamlEntityDescriptor samlIdentityProviderDescriptor = keycloakRestApi.getSamlIdentityProviderDescriptor(identityProviderRealmName);
            {
                KeycloakVoidResponse response = keycloakRestApi.addSamlIdentityProvider(
                        serviceProviderRealmName,
                        samlIdentityProviderAlias,
                        samlIdentityProviderDescriptorUrl,
                        samlIdentityProviderDescriptor,
                        true,
                        SAML_FIRST_BROKER_LOGIN_FLOW_ALIAS
                );
                assertNotNull(response);
                assertEquals(201, response.statusCode);
                assertNotNull(response.location);
                // Keycloak does not return the created User
            }
        }

        try(KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                keycloakUrl,
                identityProviderRealmName,
                realmAdminUsername,
                realmAdminPassword
        )) {

            String samlEmailScopeLocation;
            {
                KeycloakVoidResponse response = keycloakRestApi.configureSamlEmailScope(identityProviderRealmName);
                assertNotNull(response);
                assertEquals(201, response.statusCode);
                assertNotNull(response.location);
                // Keycloak does not return the created User

                samlEmailScopeLocation = response.location;
            }
            {
                KeycloakVoidResponse response = keycloakRestApi.configureSamlEmailScopeMapper(samlEmailScopeLocation);
                assertNotNull(response);
                assertEquals(201, response.statusCode);
                assertNotNull(response.location);
                // Keycloak does not return the created User
            }
            Map<String, Object> samlEmailScope;
            {
                KeycloakMapResponse response = keycloakRestApi.getByLocation(samlEmailScopeLocation);
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                samlEmailScope = response.body;
                logger.debug(keycloakRestApi.toString(samlEmailScope));
            }

            // Get the SAML IDP Descriptor from the Service Provider realm and user it to create a client in the Identity Provider realm.
            SamlEntityDescriptor samlServiceProviderDescriptor = keycloakRestApi.getSamlServiceProviderDescriptor(serviceProviderRealmName, samlIdentityProviderAlias);
            String samlServiceProviderClientLocation;
            {
                KeycloakVoidResponse response = keycloakRestApi.createSamlClient(
                        identityProviderRealmName,
                        samlServiceProviderDescriptor
                );
                assertNotNull(response);
                assertEquals(201, response.statusCode);
                assertNotNull(response.location);
                // Keycloak does not return the created User

                samlServiceProviderClientLocation = response.location;
            }
            {
                KeycloakVoidResponse response = keycloakRestApi.addDefaultClientScope(samlServiceProviderClientLocation, (String) samlEmailScope.get("id"));
                assertNotNull(response);
                assertEquals(204, response.statusCode);
            }
        }
    }

    private String randomString(int length) {
        String CHARS = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder builder = new StringBuilder();
        Random rnd = new Random();
        while (builder.length() < length) { // length of the random string.
            int index = (int) (rnd.nextFloat() * CHARS.length());
            builder.append(CHARS.charAt(index));
        }
        return builder.toString();
    }

    private List<Map<String, String>>  createBasicUsers(String realmName, int userCount) throws IOException {
        Queue<Map<String, String>> results = new ConcurrentLinkedQueue<>();

        try(KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                keycloakUrl,
                realmName,
                realmAdminUsername,
                realmAdminPassword
        )) {

            Iterable<Integer> iterable = () -> IntStream.range(0, userCount).iterator();
            Stream<Integer> stream = StreamSupport.stream(iterable.spliterator(), true);
            stream.forEach(i -> {

                String firstName = randomString(8);
                String lastName = randomString(12);
                String username = String.format("%s.%s", firstName, lastName);
                String email = getEmail(username, realmName);
                boolean emailVerifiedInd = false;
                boolean enabledInd = true;

                String userEntityId;
                {
                    KeycloakVoidResponse response = keycloakRestApi.createUser(
                            realmName,
                            username,
                            email,
                            emailVerifiedInd,
                            firstName,
                            lastName,
                            enabledInd,
                            null
                    );
                    assertNotNull(response);
                    assertEquals(201, response.statusCode);
                    assertNotNull(response.location);
                    // Keycloak does not return the created User

                    // Extract the entity ID from the location so that we do not need to make a call to fetch the user
                    int index = response.location.lastIndexOf('/') + 1;
                    userEntityId = response.location.substring(index);
                }
                {
                    KeycloakVoidResponse response = keycloakRestApi.resetPassword(
                            realmName,
                            userEntityId,
                            userPassword,
                            false
                    );
                    assertNotNull(response);
                    assertEquals(204, response.statusCode);
                }

                Map<String, String> userMap = new HashMap<>();
                userMap.put("realmName", realmName);
                userMap.put("firstName", firstName);
                userMap.put("lastName", lastName);
                userMap.put("username", username);
                userMap.put("email", email);

                results.add(userMap);
            });
        }

        return results.stream().toList();
    }

    private void configureCustomBrowserFlow(String realmName) throws IOException {

        try(KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                keycloakUrl,
                realmName,
                realmAdminUsername,
                realmAdminPassword
        )) {

            String customBrowserFlowLocation;
            {
                KeycloakVoidResponse response = keycloakRestApi.createAuthenticationFLow(
                        realmName,
                        IDP_SELECTOR_BROWSER_FLOW_ALIAS,
                        IDP_SELECTOR_BROWSER_FLOW_ALIAS,
                        BASIC_FLOW_PROVIDER_ID,
                        true
                );
                assertNotNull(response);
                assertEquals(201, response.statusCode);
                assertNotNull(response.location);

                customBrowserFlowLocation = response.location;
            }
            Map<String, Object> customBrowserFlow;
            {
                KeycloakMapResponse response = keycloakRestApi.getByLocation(customBrowserFlowLocation);
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                customBrowserFlow = response.body;
                logger.debug(keycloakRestApi.toString(customBrowserFlow));
            }
            {
                KeycloakVoidResponse response = keycloakRestApi.addAuthenticationFlowExecutionExecution(
                        realmName,
                        IDP_SELECTOR_BROWSER_FLOW_ALIAS,
                        AUTH_COOKIE_PROVIDER_ID
                );
                assertNotNull(response);
                assertEquals(201, response.statusCode);
                assertNotNull(response.location);
            }
            Map<String, Object> authCookieFlowExecution;
            {
                KeycloakListResponse response = keycloakRestApi.listAuthenticationFlowExecutions(
                        realmName,
                        IDP_SELECTOR_BROWSER_FLOW_ALIAS
                );
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                List<Map<String, Object>> results = response.body;
                logger.debug(keycloakRestApi.toString(results));

                authCookieFlowExecution = results.stream()
                        .filter(authenticationFlow -> AUTH_COOKIE_PROVIDER_ID.equals(authenticationFlow.get("providerId")))
                        .findAny()
                        .orElse(null);
            }
            assertNotNull(authCookieFlowExecution);
            {
                authCookieFlowExecution.put("requirement", "ALTERNATIVE");
                KeycloakVoidResponse response = keycloakRestApi.updateAuthenticationFlowExecution(
                        realmName,
                        IDP_SELECTOR_BROWSER_FLOW_ALIAS,
                        authCookieFlowExecution
                );
                assertNotNull(response);
                assertEquals(202, response.statusCode);
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            {
                {
                    KeycloakVoidResponse response = keycloakRestApi.addAuthenticationFlowExecutionFlow(
                            realmName,
                            IDP_SELECTOR_BROWSER_FLOW_ALIAS,
                            EXTERNAL_PROVIDER_SUB_FLOW_ALIAS,
                            null,
                            REGISTRATION_PAGE_FORM_PROVIDER_ID,
                            BASIC_FLOW_PROVIDER_ID
                    );
                    assertNotNull(response);
                    assertEquals(201, response.statusCode);
                    assertNotNull(response.location);
                }
                {
                    KeycloakVoidResponse response = keycloakRestApi.addAuthenticationFlowExecutionExecution(
                            realmName,
                            EXTERNAL_PROVIDER_SUB_FLOW_ALIAS,
                            AUTH_USERNAME_FORM_PROVIDER_ID
                    );
                    assertNotNull(response);
                    assertEquals(201, response.statusCode);
                    assertNotNull(response.location);
                }
                {
                    KeycloakVoidResponse response = keycloakRestApi.addAuthenticationFlowExecutionExecution(
                            realmName,
                            EXTERNAL_PROVIDER_SUB_FLOW_ALIAS,
                            USER_IDENTITY_PROVIDER_REDIRECT_PROVIDER_ID
                    );
                    assertNotNull(response);
                    assertEquals(201, response.statusCode);
                    assertNotNull(response.location);
                }
                Map<String, Object> externalProviderSubFlow;
                Map<String, Object> userIdentityProviderRedirect;
                {
                    KeycloakListResponse response = keycloakRestApi.listAuthenticationFlowExecutions(
                            realmName,
                            IDP_SELECTOR_BROWSER_FLOW_ALIAS
                    );
                    assertNotNull(response);
                    assertEquals(200, response.statusCode);
                    assertNotNull(response.body);
                    List<Map<String, Object>> results = response.body;
                    logger.debug(keycloakRestApi.toString(results));

                    externalProviderSubFlow = results.stream()
                            .filter(authenticationFlow -> EXTERNAL_PROVIDER_SUB_FLOW_ALIAS.equals(authenticationFlow.get("displayName")))
                            .findAny()
                            .orElse(null);

                    userIdentityProviderRedirect = results.stream()
                            .filter(authenticationFlow -> USER_IDENTITY_PROVIDER_REDIRECT_PROVIDER_ID.equals(authenticationFlow.get("providerId")))
                            .findAny()
                            .orElse(null);
                }
                assertNotNull(externalProviderSubFlow);
                assertNotNull(userIdentityProviderRedirect);
                {
                    externalProviderSubFlow.put("requirement", "ALTERNATIVE");
                    KeycloakVoidResponse response = keycloakRestApi.updateAuthenticationFlowExecution(
                            realmName,
                            IDP_SELECTOR_BROWSER_FLOW_ALIAS,
                            externalProviderSubFlow

                    );
                    assertNotNull(response);
                    assertEquals(202, response.statusCode);
                }
                {
                    userIdentityProviderRedirect.put("requirement", "REQUIRED");
                    KeycloakVoidResponse response = keycloakRestApi.updateAuthenticationFlowExecution(
                            realmName,
                            EXTERNAL_PROVIDER_SUB_FLOW_ALIAS,
                            userIdentityProviderRedirect

                    );
                    assertNotNull(response);
                    assertEquals(202, response.statusCode);
                }
            }
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            {
                {
                    KeycloakVoidResponse response = keycloakRestApi.addAuthenticationFlowExecutionFlow(
                            realmName,
                            IDP_SELECTOR_BROWSER_FLOW_ALIAS,
                            BASIC_CREDENTIALS_SUB_FLOW_ALIAS,
                            null,
                            IntegrationTests.REGISTRATION_PAGE_FORM_PROVIDER_ID,
                            BASIC_FLOW_PROVIDER_ID
                    );
                    assertNotNull(response);
                    assertEquals(201, response.statusCode);
                    assertNotNull(response.location);
                }
                {
                    KeycloakVoidResponse response = keycloakRestApi.addAuthenticationFlowExecutionExecution(
                            realmName,
                            BASIC_CREDENTIALS_SUB_FLOW_ALIAS,
                            AUTH_PASSWORD_FORM_PROVIDER_ID
                    );
                    assertNotNull(response);
                    assertEquals(201, response.statusCode);
                    assertNotNull(response.location);
                }
                Map<String, Object> basicCredentialsSubFlow;
                Map<String, Object> authPasswordForm;
                {
                    KeycloakListResponse response = keycloakRestApi.listAuthenticationFlowExecutions(
                            realmName,
                            IDP_SELECTOR_BROWSER_FLOW_ALIAS
                    );
                    assertNotNull(response);
                    assertEquals(200, response.statusCode);
                    assertNotNull(response.body);
                    List<Map<String, Object>> results = response.body;
                    logger.debug(keycloakRestApi.toString(results));

                    basicCredentialsSubFlow = results.stream()
                            .filter(authenticationFlow -> BASIC_CREDENTIALS_SUB_FLOW_ALIAS.equals(authenticationFlow.get("displayName")))
                            .findAny()
                            .orElse(null);

                    authPasswordForm = results.stream()
                            .filter(authenticationFlow -> AUTH_PASSWORD_FORM_PROVIDER_ID.equals(authenticationFlow.get("providerId")))
                            .findAny()
                            .orElse(null);
                }
                assertNotNull(basicCredentialsSubFlow);
                assertNotNull(authPasswordForm);
                {
                    basicCredentialsSubFlow.put("requirement", "ALTERNATIVE");
                    KeycloakVoidResponse response = keycloakRestApi.updateAuthenticationFlowExecution(
                            realmName,
                            IDP_SELECTOR_BROWSER_FLOW_ALIAS,
                            basicCredentialsSubFlow

                    );
                    assertNotNull(response);
                    assertEquals(202, response.statusCode);
                }
                {
                    authPasswordForm.put("requirement", "REQUIRED");
                    KeycloakVoidResponse response = keycloakRestApi.updateAuthenticationFlowExecution(
                            realmName,
                            BASIC_CREDENTIALS_SUB_FLOW_ALIAS,
                            authPasswordForm

                    );
                    assertNotNull(response);
                    assertEquals(202, response.statusCode);
                }
            }
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////

            {
                KeycloakListResponse response = keycloakRestApi.listAuthenticationFlows(realmName);
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                List<Map<String, Object>> results = response.body;
                logger.debug(keycloakRestApi.toString(results));

                customBrowserFlow = results.stream()
                        .filter(authenticationFlow -> IDP_SELECTOR_BROWSER_FLOW_ALIAS.equals(authenticationFlow.get("alias")))
                        .findAny()
                        .orElse(null);
            }
            assertNotNull(customBrowserFlow);
            {
                KeycloakMapResponse response = keycloakRestApi.getAuthenticationFlow(realmName, (String) customBrowserFlow.get("id"));
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                Map<String, Object> result = response.body;
                logger.debug(keycloakRestApi.toString(result));
            }

            // Set the Custom Browser Flow as the realms browser flow
            Map<String, Object> realm;
            {
                KeycloakMapResponse response = keycloakRestApi.getRealmByName(realmName);
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                realm = response.body;
                logger.debug(keycloakRestApi.toString(realm));
            }
            {
                realm.put("browserFlow", IDP_SELECTOR_BROWSER_FLOW_ALIAS);
                KeycloakVoidResponse response = keycloakRestApi.updateRealm(
                        realmName,
                        realm
                );
                assertNotNull(response);
                assertEquals(204, response.statusCode);
            }
        }
    }

    private void configureCustomFirstBrokerLoginFlow(String realmName) throws IOException {

        try(KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                keycloakUrl,
                realmName,
                realmAdminUsername,
                realmAdminPassword
        )) {

            String customFirstBrokerLoginFlowLocation;
            {
                KeycloakVoidResponse response = keycloakRestApi.createAuthenticationFLow(
                        realmName,
                        SAML_FIRST_BROKER_LOGIN_FLOW_ALIAS,
                        SAML_FIRST_BROKER_LOGIN_FLOW_ALIAS,
                        BASIC_FLOW_PROVIDER_ID,
                        true
                );
                assertNotNull(response);
                assertEquals(201, response.statusCode);
                assertNotNull(response.location);

                customFirstBrokerLoginFlowLocation = response.location;
            }
            Map<String, Object> customFirstBrokerLoginFlow;
            {
                KeycloakMapResponse response = keycloakRestApi.getByLocation(customFirstBrokerLoginFlowLocation);
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                customFirstBrokerLoginFlow = response.body;
                logger.debug(keycloakRestApi.toString(customFirstBrokerLoginFlow));
            }
            {
                KeycloakVoidResponse response = keycloakRestApi.addAuthenticationFlowExecutionExecution(
                        realmName,
                        SAML_FIRST_BROKER_LOGIN_FLOW_ALIAS,
                        SAML_IDP_DETECT_EXISTING_USER_PROVIDER_ID
                );
                assertNotNull(response);
                assertEquals(201, response.statusCode);
                assertNotNull(response.location);
            }
            Map<String, Object> detectExistingBrokerUserFlowExecution;
            {
                KeycloakListResponse response = keycloakRestApi.listAuthenticationFlowExecutions(
                        realmName,
                        SAML_FIRST_BROKER_LOGIN_FLOW_ALIAS
                );
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                List<Map<String, Object>> results = response.body;
                logger.debug(keycloakRestApi.toString(results));

                detectExistingBrokerUserFlowExecution = results.stream()
                        .filter(authenticationFlow -> SAML_IDP_DETECT_EXISTING_USER_PROVIDER_ID.equals(authenticationFlow.get("providerId")))
                        .findAny()
                        .orElse(null);
            }
            assertNotNull(detectExistingBrokerUserFlowExecution);
            {
                detectExistingBrokerUserFlowExecution.put("requirement", "REQUIRED");
                KeycloakVoidResponse response = keycloakRestApi.updateAuthenticationFlowExecution(
                        realmName,
                        SAML_FIRST_BROKER_LOGIN_FLOW_ALIAS,
                        detectExistingBrokerUserFlowExecution
                );
                assertNotNull(response);
                assertEquals(202, response.statusCode);
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            {
                KeycloakVoidResponse response = keycloakRestApi.addAuthenticationFlowExecutionExecution(
                        realmName,
                        SAML_FIRST_BROKER_LOGIN_FLOW_ALIAS,
                        IDP_AUTO_LINK_PROVIDER_ID
                );
                assertNotNull(response);
                assertEquals(201, response.statusCode);
                assertNotNull(response.location);
            }
            Map<String, Object> autoLinkBrokerUserFlowExecution;
            {
                KeycloakListResponse response = keycloakRestApi.listAuthenticationFlowExecutions(
                        realmName,
                        SAML_FIRST_BROKER_LOGIN_FLOW_ALIAS
                );
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                List<Map<String, Object>> results = response.body;
                logger.debug(keycloakRestApi.toString(results));

                autoLinkBrokerUserFlowExecution = results.stream()
                        .filter(authenticationFlow -> IDP_AUTO_LINK_PROVIDER_ID.equals(authenticationFlow.get("providerId")))
                        .findAny()
                        .orElse(null);
            }
            assertNotNull(autoLinkBrokerUserFlowExecution);
            {
                autoLinkBrokerUserFlowExecution.put("requirement", "REQUIRED");
                KeycloakVoidResponse response = keycloakRestApi.updateAuthenticationFlowExecution(
                        realmName,
                        SAML_FIRST_BROKER_LOGIN_FLOW_ALIAS,
                        autoLinkBrokerUserFlowExecution
                );
                assertNotNull(response);
                assertEquals(202, response.statusCode);
            }
        }
    }

    private void configureSamlDirectGrantFlow(String realmName) throws IOException {

        try(KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                keycloakUrl,
                realmName,
                realmAdminUsername,
                realmAdminPassword
        )) {

            String saml2DirectGrantFlowLocation;
            {
                KeycloakVoidResponse response = keycloakRestApi.createAuthenticationFLow(
                        realmName,
                        SAML2_DIRECT_GRANT_FLOW_ALIAS,
                        SAML2_DIRECT_GRANT_FLOW_ALIAS,
                        BASIC_FLOW_PROVIDER_ID,
                        true
                );
                assertNotNull(response);
                assertEquals(201, response.statusCode);
                assertNotNull(response.location);

                saml2DirectGrantFlowLocation = response.location;
            }
            Map<String, Object> saml2DirectGrantFlow;
            {
                KeycloakMapResponse response = keycloakRestApi.getByLocation(saml2DirectGrantFlowLocation);
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                saml2DirectGrantFlow = response.body;
                logger.debug(keycloakRestApi.toString(saml2DirectGrantFlow));
            }
            {
                KeycloakVoidResponse response = keycloakRestApi.addAuthenticationFlowExecutionExecution(
                        realmName,
                        SAML2_DIRECT_GRANT_FLOW_ALIAS,
                        DIRECT_GRANT_VALIDATE_USERNAME_PROVIDER_ID
                );
                assertNotNull(response);
                assertEquals(201, response.statusCode);
                assertNotNull(response.location);
            }
            Map<String, Object> directGrantValidateUsernameFlowExecution;
            {
                KeycloakListResponse response = keycloakRestApi.listAuthenticationFlowExecutions(
                        realmName,
                        SAML2_DIRECT_GRANT_FLOW_ALIAS
                );
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                List<Map<String, Object>> results = response.body;
                logger.debug(keycloakRestApi.toString(results));

                directGrantValidateUsernameFlowExecution = results.stream()
                        .filter(authenticationFlow -> DIRECT_GRANT_VALIDATE_USERNAME_PROVIDER_ID.equals(authenticationFlow.get("providerId")))
                        .findAny()
                        .orElse(null);
            }
            assertNotNull(directGrantValidateUsernameFlowExecution);
            {
                directGrantValidateUsernameFlowExecution.put("requirement", "REQUIRED");
                KeycloakVoidResponse response = keycloakRestApi.updateAuthenticationFlowExecution(
                        realmName,
                        SAML2_DIRECT_GRANT_FLOW_ALIAS,
                        directGrantValidateUsernameFlowExecution
                );
                assertNotNull(response);
                assertEquals(202, response.statusCode);
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            {
                KeycloakVoidResponse response = keycloakRestApi.addAuthenticationFlowExecutionExecution(
                        realmName,
                        SAML2_DIRECT_GRANT_FLOW_ALIAS,
                        DIRECT_GRANT_VALIDATE_SAML2_PROVIDER_ID
                );
                assertNotNull(response);
                assertEquals(201, response.statusCode);
                assertNotNull(response.location);
            }
            Map<String, Object> directGrantValidateSaml2FlowExecution;
            {
                KeycloakListResponse response = keycloakRestApi.listAuthenticationFlowExecutions(
                        realmName,
                        SAML2_DIRECT_GRANT_FLOW_ALIAS
                );
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                List<Map<String, Object>> results = response.body;
                logger.debug(keycloakRestApi.toString(results));

                directGrantValidateSaml2FlowExecution = results.stream()
                        .filter(authenticationFlow -> DIRECT_GRANT_VALIDATE_SAML2_PROVIDER_ID.equals(authenticationFlow.get("providerId")))
                        .findAny()
                        .orElse(null);
            }
            assertNotNull(directGrantValidateSaml2FlowExecution);
            {
                directGrantValidateSaml2FlowExecution.put("requirement", "REQUIRED");
                KeycloakVoidResponse response = keycloakRestApi.updateAuthenticationFlowExecution(
                        realmName,
                        SAML2_DIRECT_GRANT_FLOW_ALIAS,
                        directGrantValidateSaml2FlowExecution
                );
                assertNotNull(response);
                assertEquals(202, response.statusCode);
            }
        }
    }

    private String getEmail(String username, String realmName) {
        return String.format("%s@%s.com", username, realmName.replace("-", "").replace("_", "").substring(0, 10));
    }

    private void createRealmAdminUser(
            String realmName
    ) throws IOException {

        try(KeycloakRestApi keycloakRestApi = new KeycloakRestApi(
                keycloakUrl,
                keycloakMasterRealmName,
                masterAdminUsername,
                masterAdminPassword
        )) {
            String userLocation;
            {
                KeycloakVoidResponse response = keycloakRestApi.createUser(
                        realmName,
                        realmAdminUsername,
                        getEmail("realm.admin", realmName),
                        false,
                        "Realm",
                        "Admin",
                        true,
                        null
                );
                assertNotNull(response);
                assertEquals(201, response.statusCode);
                assertNotNull(response.location);
                // Keycloak does not return the created User

                userLocation = response.location;
            }
            Map<String, Object> user;
            {
                KeycloakMapResponse response = keycloakRestApi.getByLocation(userLocation);
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                user = response.body;
                logger.debug(keycloakRestApi.toString(user));
            }
            Map<String, Object> realmAdminRole;
            {
                KeycloakListResponse response = keycloakRestApi.listClientRolesAvailableToUser(realmName, (String) user.get("id"), "realm-admin");
                assertNotNull(response);
                assertEquals(200, response.statusCode);
                assertNotNull(response.body);
                List<Map<String, Object>> results = response.body;
                logger.debug(keycloakRestApi.toString(results));

                realmAdminRole = results.stream()
                        .filter(role -> REALM_ADMIN_ROLE_NAME.equals(role.get("role")) && REALM_MANAGEMENT_CLIENT_ID.equals(role.get("client")))
                        .findAny()
                        .orElse(null);
                assertNotNull(realmAdminRole);
            }
            {
                KeycloakVoidResponse response = keycloakRestApi.assignClientRole(
                        realmName,
                        (String) user.get("id"),
                        realmAdminRole
                );
                assertNotNull(response);
                assertEquals(204, response.statusCode);
            }
            {
                KeycloakVoidResponse response = keycloakRestApi.resetPassword(
                        realmName,
                        (String) user.get("id"),
                        realmAdminPassword,
                        false
                );
                assertNotNull(response);
                assertEquals(204, response.statusCode);
            }
        }
    }

    @Test
    public void testSamlXmlMapping() throws JsonProcessingException {

        XmlMapper mapper = new XmlMapper();

        String xml = "<md:EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" entityID=\"https://custom-keycloak.192.168.105.3.nip.io/realms/saml_50c3d9b0-3bb8-4f9b-8e48-a6d89438ad8b\"><md:IDPSSODescriptor WantAuthnRequestsSigned=\"true\" protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\"><md:KeyDescriptor use=\"signing\"><ds:KeyInfo><ds:KeyName>JjyxX7Q7J9DEYnvYRxFkUw2qrlwid82mBlHv2rMFIm0</ds:KeyName><ds:X509Data><ds:X509Certificate>MIIC4TCCAckCBgGOYxBjmDANBgkqhkiG9w0BAQsFADA0MTIwMAYDVQQDDClzYW1sXzUwYzNkOWIwLTNiYjgtNGY5Yi04ZTQ4LWE2ZDg5NDM4YWQ4YjAeFw0yNDAzMjEyMjA4MjJaFw0zNDAzMjEyMjEwMDJaMDQxMjAwBgNVBAMMKXNhbWxfNTBjM2Q5YjAtM2JiOC00ZjliLThlNDgtYTZkODk0MzhhZDhiMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApx8Is9z5AZJt+PQJpM3viPIEMPh9vrYdtmzhv3tY0uVJecThi1zQt9M1QZ+3/WNMPVXf/vUWbUFuwvqYA8g198Q1OCGK5Wywn89xXyBanruJgoJQHVSkQHRH79A7sHSScqo2oM+utDkQHg1mqISp9VLVLK1QmeYp3iAtI7Uq69qR4HIgxA8w9XqDy9jJtzkFKRE8vwcgAXO6dj1LsdjhG9hyAujinYb7/B4/jVQ3uRW07pg8SvIuE/9expKD9MrapiL4H1svCRkJnfqZbhYGM30YZ53Bt9RWpIm8jESxvBb2qXEJ/GY5V1zKG7PSzuSwVsavEJn6xx2JZWFcD47+YQIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQCFxjak2HlG8iyJY7lQE7Sciu4NCGy8X4h6kQO9RpZguqNDXsmFj8x3/YRUYmzYrzUBbuivYjuvuByp+wtMMuy1oFuf79NxVg7h2EH7tBnGo0J+TLxjOQfbdIaP5beybdeBHRqudQChSuuwzOQ8s3PGGxEC4QosTsvyJpg6jpZKkJVgtpW982ZUIc9J6mGhPYgDxJg7MnE5rzbtQlqrqHg3NtkAviNhT/Y4p8WWM1rL80k18q2Yr36zdqpAmUuLg9S2U8btAeTTfMCydG7ULja472QkzhNKs49u6ItrXRxxP9xCpEOhwLqMTEW1vJ+Uv5ZYUWj4OgrA4aehpmExh6Z/</ds:X509Certificate></ds:X509Data></ds:KeyInfo></md:KeyDescriptor><md:ArtifactResolutionService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\" Location=\"https://custom-keycloak.192.168.105.3.nip.io/realms/saml_50c3d9b0-3bb8-4f9b-8e48-a6d89438ad8b/protocol/saml/resolve\" index=\"0\"></md:ArtifactResolutionService><md:SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"https://custom-keycloak.192.168.105.3.nip.io/realms/saml_50c3d9b0-3bb8-4f9b-8e48-a6d89438ad8b/protocol/saml\"></md:SingleLogoutService><md:SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"https://custom-keycloak.192.168.105.3.nip.io/realms/saml_50c3d9b0-3bb8-4f9b-8e48-a6d89438ad8b/protocol/saml\"></md:SingleLogoutService><md:SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact\" Location=\"https://custom-keycloak.192.168.105.3.nip.io/realms/saml_50c3d9b0-3bb8-4f9b-8e48-a6d89438ad8b/protocol/saml\"></md:SingleLogoutService><md:SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\" Location=\"https://custom-keycloak.192.168.105.3.nip.io/realms/saml_50c3d9b0-3bb8-4f9b-8e48-a6d89438ad8b/protocol/saml\"></md:SingleLogoutService><md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:persistent</md:NameIDFormat><md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:transient</md:NameIDFormat><md:NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified</md:NameIDFormat><md:NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress</md:NameIDFormat><md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"https://custom-keycloak.192.168.105.3.nip.io/realms/saml_50c3d9b0-3bb8-4f9b-8e48-a6d89438ad8b/protocol/saml\"></md:SingleSignOnService><md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"https://custom-keycloak.192.168.105.3.nip.io/realms/saml_50c3d9b0-3bb8-4f9b-8e48-a6d89438ad8b/protocol/saml\"></md:SingleSignOnService><md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\" Location=\"https://custom-keycloak.192.168.105.3.nip.io/realms/saml_50c3d9b0-3bb8-4f9b-8e48-a6d89438ad8b/protocol/saml\"></md:SingleSignOnService><md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact\" Location=\"https://custom-keycloak.192.168.105.3.nip.io/realms/saml_50c3d9b0-3bb8-4f9b-8e48-a6d89438ad8b/protocol/saml\"></md:SingleSignOnService></md:IDPSSODescriptor></md:EntityDescriptor>\n" +
                "2024-03-21 15:10:03 DEBUG PoolingHttpClientConnectionMan";

        logger.debug("\n"+xml);

        mapper.readValue(xml, SamlEntityDescriptor.class);
    }

    @Test
    public void testSamlXmlMapping2() throws JsonProcessingException {

        XmlMapper mapper = (XmlMapper) new XmlMapper().enable(SerializationFeature.INDENT_OUTPUT);

        SamlEntityDescriptor samlDescriptor = new SamlEntityDescriptor();
        samlDescriptor.entityId = "http://192.168.105.3:31334/realms/saml_618c4019-249f-4aa1-88ce-bdf4dce87d58";

        SamlEntityDescriptor.IdpssoDescriptor idpssoDescriptor = new SamlEntityDescriptor.IdpssoDescriptor();
        idpssoDescriptor.wantAuthnRequestsSigned = "true";
        idpssoDescriptor.protocolSupportEnumeration = "urn:oasis:names:tc:SAML:2.0:protocol";
        SamlEntityDescriptor.KeyDescriptor keyDescriptor = new SamlEntityDescriptor.KeyDescriptor();
        keyDescriptor.use = "signing";
        SamlEntityDescriptor.KeyDescriptor.KeyInfo keyInfo = new SamlEntityDescriptor.KeyDescriptor.KeyInfo();
        keyInfo.keyName = "Is1dK3-YspqDhE4WLp4-ltHb_xw3gnujqHU8E05hDwE";
        SamlEntityDescriptor.KeyDescriptor.KeyInfo.X509Data x509Data = new SamlEntityDescriptor.KeyDescriptor.KeyInfo.X509Data();
        x509Data.x509Certificate = "MIIC4TCCAckCBgGOYssnDDANBgkqhkiG9w0BAQsFADA0MTIwMAYDVQQDDClzYW1sXzYxOGM0MDE5LTI0OWYtNGFhMS04OGNlLWJkZjRkY2U4N2Q1ODAeFw0yNDAzMjEyMDUyNDRaFw0zNDAzMjEyMDU0MjRaMDQxMjAwBgNVBAMMKXNhbWxfNjE4YzQwMTktMjQ5Zi00YWExLTg4Y2UtYmRmNGRjZTg3ZDU4MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4DL/hOFlpP+pbxezmWCiaMqhe3eoQhPuHX71Abv8qrFsfLhv5+IiJ8YgWUwWa3sayxxvZOp/dyPG4Dsfc5+lS36NTkQ5/CKs23xwoECVj+pWWeW7QJmD53Hdpo2LKX0IUxQS2g6BUDjT8wmZSdMv9g/7poIXO32acxrMEZANNMFPF0c1aGsDFN2xqJQa2DxeaWXEDi5QcmPZiZtUD9EnoX1E17nVf/bEBDPn7p7bNh7IojTQMWDAcNB9oUFxdihW3nnWPYFq0qGxcBMOhUmSxjleUmV9c3yuVBSOzGkqiFI6Dif3U6b/TY57tcRE4CRW+w0tJPInbFT3amU1K5Xf6QIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQBoFDVvCICkwSgr0b95msO1cTWrqGOp9WoMVwEQOlBSeucLx47VU6BAEGOLRo1zW0mB1xHlRealh1IjCvyoDArtP8nFBx/YoL8zXNefSubJgUuMxHo7xhEt0p+S4si+T+WaTbyECkh8hmYq5jn+ABz3qUo+tHWrBDUB8DVFXOiS0I9qlZTcbx7C2nzlVjnKCK9U0jikJmO8zOsbuTeplEYJ71iS+MixUXNnFItGxpm9P8kFU3Ax895BiMMRFLFN5Y/QkvimkMf/XqeqVwgNSCYMBWAFZRqH/qwOkVIlZ5BwGAX/w2FDshLiT7DwUbCiUbSYqZGfLZbCUoCAn1VeJ40U";
        keyInfo.x509Datas.add(x509Data);
        keyDescriptor.keyInfos.add(keyInfo);
        idpssoDescriptor.keyDescriptors.add(keyDescriptor);
        SamlEntityDescriptor.ArtifactResolutionService artifactResolutionService = new SamlEntityDescriptor.ArtifactResolutionService();
        artifactResolutionService.binding = "urn:oasis:names:tc:SAML:2.0:bindings:SOAP";
        artifactResolutionService.location = "http://192.168.105.3:31334/realms/saml_618c4019-249f-4aa1-88ce-bdf4dce87d58/protocol/saml/resolve";
        artifactResolutionService.index = "0";
        idpssoDescriptor.artifactResolutionServices.add(artifactResolutionService);
        SamlEntityDescriptor.SingleLogoutService singleLogoutService1 = new SamlEntityDescriptor.SingleLogoutService();
        singleLogoutService1.binding = "urn:oasis:names:tc:SAML:2.0:bindings:SOAP";
        singleLogoutService1.location = "http://192.168.105.3:31334/realms/saml_618c4019-249f-4aa1-88ce-bdf4dce87d58/protocol/saml";
        idpssoDescriptor.singleLogoutServices.add(singleLogoutService1);
        SamlEntityDescriptor.SingleLogoutService singleLogoutService2 = new SamlEntityDescriptor.SingleLogoutService();
        singleLogoutService2.binding = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";
        singleLogoutService2.location = "http://192.168.105.3:31334/realms/saml_618c4019-249f-4aa1-88ce-bdf4dce87d58/protocol/saml";
        idpssoDescriptor.singleLogoutServices.add(singleLogoutService2);
        idpssoDescriptor.nameIDFormats.add("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
        idpssoDescriptor.nameIDFormats.add("urn:oasis:names:tc:SAML:2.0:nameid-format:transient");
        idpssoDescriptor.nameIDFormats.add("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
        idpssoDescriptor.nameIDFormats.add("urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress");
        samlDescriptor.idpssoDescriptors.add(idpssoDescriptor);

        String xml = mapper.writeValueAsString(samlDescriptor);

        logger.debug("\n"+xml);
    }
}