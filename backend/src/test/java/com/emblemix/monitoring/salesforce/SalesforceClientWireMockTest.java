package com.emblemix.monitoring.salesforce;

import com.emblemix.monitoring.salesforce.dto.SalesforceLimitsDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class SalesforceClientWireMockTest {

    private static WireMockServer wireMockServer;
    private static SalesforceClient salesforceClient;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        salesforceClient = new SalesforceClient(objectMapper);
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("Should successfully exchange authorization code for tokens via WireMock mock")
    void testExchangeCodeSuccess() throws Exception {
        String tokenEndpoint = "http://localhost:" + wireMockServer.port() + "/services/oauth2/token";

        stubFor(post(urlEqualTo("/services/oauth2/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "access_token": "mock-access-token-12345",
                                  "refresh_token": "mock-refresh-token-67890",
                                  "instance_url": "http://localhost:%d",
                                  "id": "https://login.salesforce.com/id/00D5g000004XXXX/0055g00000XXXXX",
                                  "token_type": "Bearer"
                                }
                                """.formatted(wireMockServer.port()))));

        JsonNode response = salesforceClient.exchangeCode(
                tokenEndpoint, "test-client-id", "test-client-secret",
                "http://localhost:5173/callback", "auth-code-123"
        );

        assertNotNull(response);
        assertEquals("mock-access-token-12345", response.path("access_token").asText());
        assertEquals("mock-refresh-token-67890", response.path("refresh_token").asText());
    }

    @Test
    @DisplayName("Should fetch and parse Salesforce DailyApiRequests limits accurately")
    void testFetchApiLimits() throws Exception {
        String instanceUrl = "http://localhost:" + wireMockServer.port();

        stubFor(get(urlEqualTo("/services/data/v61.0/limits"))
                .withHeader("Authorization", equalTo("Bearer mock-access-token-12345"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "DailyApiRequests": {
                                    "Max": 100000,
                                    "Remaining": 15000
                                  }
                                }
                                """)));

        SalesforceLimitsDto limits = salesforceClient.getApiLimits(instanceUrl, "mock-access-token-12345", "v61.0");

        assertNotNull(limits);
        assertEquals(100000L, limits.max());
        assertEquals(15000L, limits.remaining());
        // 85000 used / 100000 = 85.0%
        assertEquals(85.0, limits.percentageUsed());
    }
}
