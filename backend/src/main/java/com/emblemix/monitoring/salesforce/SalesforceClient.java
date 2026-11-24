package com.emblemix.monitoring.salesforce;

import com.emblemix.monitoring.salesforce.dto.SalesforceApexJobDto;
import com.emblemix.monitoring.salesforce.dto.SalesforceLimitsDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SalesforceClient {

    private static final Logger log = LoggerFactory.getLogger(SalesforceClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SalesforceClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public JsonNode exchangeCode(String tokenEndpoint, String clientId, String clientSecret,
                                 String redirectUri, String code) throws Exception {
        String requestBody = "grant_type=authorization_code" +
                "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenEndpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("Salesforce token exchange failed (" + response.statusCode() + "): " + response.body());
        }

        return objectMapper.readTree(response.body());
    }

    public JsonNode refreshToken(String tokenEndpoint, String clientId, String clientSecret,
                                 String refreshToken) throws Exception {
        String requestBody = "grant_type=refresh_token" +
                "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenEndpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("Salesforce token refresh failed (" + response.statusCode() + "): " + response.body());
        }

        return objectMapper.readTree(response.body());
    }

    public void revokeToken(String revokeEndpoint, String token) {
        try {
            String requestBody = "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(revokeEndpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("Failed to revoke token on Salesforce: {}", e.getMessage());
        }
    }

    public SalesforceLimitsDto getApiLimits(String instanceUrl, String accessToken, String apiVersion) throws Exception {
        String url = instanceUrl.replaceAll("/+$", "") + "/services/data/" + apiVersion + "/limits";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("Failed to fetch Salesforce API limits (" + response.statusCode() + "): " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode dailyApiRequests = root.path("DailyApiRequests");
        long max = dailyApiRequests.path("Max").asLong(15000);
        long remaining = dailyApiRequests.path("Remaining").asLong(15000);
        long used = max - remaining;
        double percentage = max > 0 ? ((double) used / (double) max) * 100.0 : 0.0;

        return new SalesforceLimitsDto(max, remaining, Math.round(percentage * 100.0) / 100.0);
    }

    public List<SalesforceApexJobDto> getFailedApexJobs(String instanceUrl, String accessToken, String apiVersion) throws Exception {
        String soql = "SELECT Id, JobType, Status, NumberOfErrors, ExtendedStatus, CreatedDate FROM AsyncApexJob WHERE Status IN ('Failed','Aborted') ORDER BY CreatedDate DESC LIMIT 20";
        String url = instanceUrl.replaceAll("/+$", "") + "/services/data/" + apiVersion +
                "/tooling/query?q=" + URLEncoder.encode(soql, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            log.warn("Tooling API query for AsyncApexJob returned {}: {}", response.statusCode(), response.body());
            return List.of();
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode records = root.path("records");
        List<SalesforceApexJobDto> jobs = new ArrayList<>();
        if (records.isArray()) {
            for (JsonNode rec : records) {
                String id = rec.path("Id").asText();
                String jobType = rec.path("JobType").asText();
                String status = rec.path("Status").asText();
                int errors = rec.path("NumberOfErrors").asInt(0);
                String extended = rec.path("ExtendedStatus").asText(null);
                String createdDateStr = rec.path("CreatedDate").asText();
                Instant createdDate = createdDateStr != null && !createdDateStr.isBlank()
                        ? Instant.parse(createdDateStr)
                        : Instant.now();

                jobs.add(new SalesforceApexJobDto(id, jobType, status, errors, extended, createdDate));
            }
        }
        return jobs;
    }

    public List<Map<String, Object>> getFailedLogins(String instanceUrl, String accessToken, String apiVersion) throws Exception {
        String soql = "SELECT Id, UserId, LoginTime, Status, SourceIp FROM LoginHistory WHERE Status != 'Success' ORDER BY LoginTime DESC LIMIT 20";
        String url = instanceUrl.replaceAll("/+$", "") + "/services/data/" + apiVersion +
                "/query?q=" + URLEncoder.encode(soql, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            return List.of();
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode records = root.path("records");
        List<Map<String, Object>> logins = new ArrayList<>();
        if (records.isArray()) {
            for (JsonNode rec : records) {
                logins.add(Map.of(
                        "id", rec.path("Id").asText(),
                        "userId", rec.path("UserId").asText(),
                        "status", rec.path("Status").asText(),
                        "loginTime", rec.path("LoginTime").asText(),
                        "sourceIp", rec.path("SourceIp").asText()
                ));
            }
        }
        return logins;
    }
}
