package com.emblemix.monitoring.salesforce;

import com.emblemix.common.exception.BadRequestException;
import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.common.security.SecurityUtils;
import com.emblemix.common.util.AesEncryptionUtil;
import com.emblemix.integration.IntegrationCredential;
import com.emblemix.integration.IntegrationCredentialRepository;
import com.emblemix.integration.IntegrationType;
import com.emblemix.monitoring.salesforce.dto.SalesforceAuthUrlResponse;
import com.emblemix.system.ConnectedSystem;
import com.emblemix.system.ConnectedSystemRepository;
import com.emblemix.system.SystemStatus;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SalesforceOAuthService {

    private static final Logger log = LoggerFactory.getLogger(SalesforceOAuthService.class);

    private final SalesforceConfigurationRepository configRepository;
    private final ConnectedSystemRepository systemRepository;
    private final IntegrationCredentialRepository credentialRepository;
    private final SalesforceClient salesforceClient;
    private final AesEncryptionUtil encryptionUtil;

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String prodLoginUrl;
    private final String sandboxLoginUrl;

    // Secure state store: stateString -> OAuthState
    private final Map<String, OAuthState> pendingStates = new ConcurrentHashMap<>();

    record OAuthState(UUID orgId, UUID systemId, boolean isSandbox, Instant createdAt) {}

    public SalesforceOAuthService(
            SalesforceConfigurationRepository configRepository,
            ConnectedSystemRepository systemRepository,
            IntegrationCredentialRepository credentialRepository,
            SalesforceClient salesforceClient,
            AesEncryptionUtil encryptionUtil,
            @Value("${emblemix.salesforce.client-id:emblemix-salesforce-client-id}") String clientId,
            @Value("${emblemix.salesforce.client-secret:emblemix-salesforce-client-secret}") String clientSecret,
            @Value("${emblemix.salesforce.redirect-uri:http://localhost:5173/integrations/salesforce/callback}") String redirectUri,
            @Value("${emblemix.salesforce.production-login-url:https://login.salesforce.com}") String prodLoginUrl,
            @Value("${emblemix.salesforce.sandbox-login-url:https://test.salesforce.com}") String sandboxLoginUrl
    ) {
        this.configRepository = configRepository;
        this.systemRepository = systemRepository;
        this.credentialRepository = credentialRepository;
        this.salesforceClient = salesforceClient;
        this.encryptionUtil = encryptionUtil;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.prodLoginUrl = prodLoginUrl;
        this.sandboxLoginUrl = sandboxLoginUrl;
    }

    public SalesforceAuthUrlResponse generateAuthorizationUrl(UUID systemId, boolean isSandbox) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();

        ConnectedSystem system = systemRepository.findByIdAndOrganizationId(systemId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConnectedSystem", "id", systemId));

        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        pendingStates.put(state, new OAuthState(orgId, system.getId(), isSandbox, Instant.now()));

        String baseLoginUrl = isSandbox ? sandboxLoginUrl : prodLoginUrl;
        String authUrl = baseLoginUrl.replaceAll("/+$", "") + "/services/oauth2/authorize?" +
                "response_type=code" +
                "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode("api refresh_token", StandardCharsets.UTF_8);

        return new SalesforceAuthUrlResponse(authUrl, state);
    }

    @Transactional
    public ConnectedSystem handleCallback(String code, String state) {
        if (state == null || !pendingStates.containsKey(state)) {
            throw new BadRequestException("Invalid or expired OAuth state parameter");
        }

        OAuthState oauthState = pendingStates.remove(state);
        // Expire state after 15 minutes
        if (oauthState.createdAt().plusSeconds(900).isBefore(Instant.now())) {
            throw new BadRequestException("OAuth state has expired");
        }

        ConnectedSystem system = systemRepository.findById(oauthState.systemId())
                .orElseThrow(() -> new ResourceNotFoundException("ConnectedSystem", "id", oauthState.systemId()));

        String baseLoginUrl = oauthState.isSandbox() ? sandboxLoginUrl : prodLoginUrl;
        String tokenEndpoint = baseLoginUrl.replaceAll("/+$", "") + "/services/oauth2/token";

        try {
            JsonNode tokenResponse = salesforceClient.exchangeCode(
                    tokenEndpoint, clientId, clientSecret, redirectUri, code
            );

            String accessToken = tokenResponse.path("access_token").asText();
            String refreshToken = tokenResponse.path("refresh_token").asText(null);
            String instanceUrl = tokenResponse.path("instance_url").asText();
            String idUrl = tokenResponse.path("id").asText();
            String tokenType = tokenResponse.path("token_type").asText("Bearer");

            // Extract org ID from id URL (e.g. https://login.salesforce.com/id/00D5g000004XXXX/...)
            String externalOrgId = null;
            if (idUrl != null && idUrl.contains("/id/")) {
                String[] parts = idUrl.split("/id/")[1].split("/");
                externalOrgId = parts[0];
            }

            // Save encrypted credentials at rest
            IntegrationCredential credential = credentialRepository
                    .findBySystemIdAndIntegrationType(system.getId(), IntegrationType.SALESFORCE)
                    .orElseGet(() -> new IntegrationCredential(
                            UUID.randomUUID(), system.getOrganization(), system,
                            IntegrationType.SALESFORCE, null, null, null, null, null
                    ));

            credential.setEncryptedAccessToken(encryptionUtil.encrypt(accessToken));
            if (refreshToken != null && !refreshToken.isBlank()) {
                credential.setEncryptedRefreshToken(encryptionUtil.encrypt(refreshToken));
            }
            credential.setInstanceUrl(instanceUrl);
            credential.setExternalOrgId(externalOrgId);
            credential.setTokenType(tokenType);
            credential.setIssuedAt(Instant.now());
            credentialRepository.save(credential);

            system.setStatus(SystemStatus.HEALTHY);
            system.setLastMonitoredAt(Instant.now());
            systemRepository.save(system);

            log.info("Salesforce integration successfully connected for system {} (Org: {})", system.getName(), externalOrgId);
            return system;

        } catch (Exception e) {
            log.error("Salesforce OAuth callback failed for system {}: {}", system.getId(), e.getMessage());
            throw new BadRequestException("Salesforce authentication failed: " + e.getMessage());
        }
    }

    @Transactional
    public void disconnect(UUID systemId) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        ConnectedSystem system = systemRepository.findByIdAndOrganizationId(systemId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConnectedSystem", "id", systemId));

        credentialRepository.findBySystemIdAndIntegrationType(system.getId(), IntegrationType.SALESFORCE)
                .ifPresent(cred -> {
                    if (cred.getEncryptedRefreshToken() != null) {
                        try {
                            String refreshToken = encryptionUtil.decrypt(cred.getEncryptedRefreshToken());
                            salesforceClient.revokeToken(prodLoginUrl + "/services/oauth2/revoke", refreshToken);
                        } catch (Exception ignored) {}
                    }
                    credentialRepository.delete(cred);
                });

        system.setStatus(SystemStatus.UNKNOWN);
        systemRepository.save(system);
        log.info("Salesforce integration disconnected for system {}", systemId);
    }
}
