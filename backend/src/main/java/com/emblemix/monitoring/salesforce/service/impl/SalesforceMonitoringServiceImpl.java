package com.emblemix.monitoring.salesforce.service.impl;

import com.emblemix.common.exception.BadRequestException;
import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.common.security.SecurityUtils;
import com.emblemix.common.util.AesEncryptionUtil;
import com.emblemix.event.Event;
import com.emblemix.event.EventSeverity;
import com.emblemix.event.EventSourceType;
import com.emblemix.event.EventType;
import com.emblemix.event.service.EventIngestionService;
import com.emblemix.integration.IntegrationCredential;
import com.emblemix.integration.IntegrationCredentialRepository;
import com.emblemix.integration.IntegrationType;
import com.emblemix.monitoring.salesforce.*;
import com.emblemix.monitoring.salesforce.dto.*;
import com.emblemix.monitoring.salesforce.service.SalesforceMonitoringService;
import com.emblemix.organization.Organization;
import com.emblemix.organization.OrganizationRepository;
import com.emblemix.system.ConnectedSystem;
import com.emblemix.system.ConnectedSystemRepository;
import com.emblemix.system.SystemStatus;
import com.emblemix.system.SystemType;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SalesforceMonitoringServiceImpl implements SalesforceMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(SalesforceMonitoringServiceImpl.class);

    private final SalesforceConfigurationRepository configRepository;
    private final ConnectedSystemRepository systemRepository;
    private final OrganizationRepository organizationRepository;
    private final IntegrationCredentialRepository credentialRepository;
    private final SalesforceClient salesforceClient;
    private final AesEncryptionUtil encryptionUtil;
    private final EventIngestionService ingestionService;

    private final String clientId;
    private final String clientSecret;
    private final String prodLoginUrl;
    private final String sandboxLoginUrl;
    private final String apiVersion;

    public SalesforceMonitoringServiceImpl(
            SalesforceConfigurationRepository configRepository,
            ConnectedSystemRepository systemRepository,
            OrganizationRepository organizationRepository,
            IntegrationCredentialRepository credentialRepository,
            SalesforceClient salesforceClient,
            AesEncryptionUtil encryptionUtil,
            EventIngestionService ingestionService,
            @Value("${emblemix.salesforce.client-id:emblemix-salesforce-client-id}") String clientId,
            @Value("${emblemix.salesforce.client-secret:emblemix-salesforce-client-secret}") String clientSecret,
            @Value("${emblemix.salesforce.production-login-url:https://login.salesforce.com}") String prodLoginUrl,
            @Value("${emblemix.salesforce.sandbox-login-url:https://test.salesforce.com}") String sandboxLoginUrl,
            @Value("${emblemix.salesforce.api-version:v61.0}") String apiVersion
    ) {
        this.configRepository = configRepository;
        this.systemRepository = systemRepository;
        this.organizationRepository = organizationRepository;
        this.credentialRepository = credentialRepository;
        this.salesforceClient = salesforceClient;
        this.encryptionUtil = encryptionUtil;
        this.ingestionService = ingestionService;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.prodLoginUrl = prodLoginUrl;
        this.sandboxLoginUrl = sandboxLoginUrl;
        this.apiVersion = apiVersion;
    }

    @Override
    @Transactional
    public SalesforceConfigResponse createSalesforceSystem(SalesforceConfigRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();

        if (systemRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, request.name().trim())) {
            throw new BadRequestException("A system with the name '" + request.name() + "' already exists");
        }

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

        ConnectedSystem system = new ConnectedSystem(
                UUID.randomUUID(),
                org,
                request.name().trim(),
                SystemType.SALESFORCE,
                request.description(),
                request.environment() != null ? request.environment() : "PRODUCTION"
        );
        ConnectedSystem savedSystem = systemRepository.save(system);

        SalesforceConfiguration config = new SalesforceConfiguration(
                UUID.randomUUID(),
                savedSystem,
                org,
                request.isSandbox() != null ? request.isSandbox() : false,
                request.apiUsageWarningThresholdPct() != null ? request.apiUsageWarningThresholdPct() : 80.0,
                request.apiUsageCriticalThresholdPct() != null ? request.apiUsageCriticalThresholdPct() : 95.0,
                request.monitorApexJobs() != null ? request.monitorApexJobs() : true,
                request.monitorLoginActivity() != null ? request.monitorLoginActivity() : true
        );
        SalesforceConfiguration savedConfig = configRepository.save(config);

        return SalesforceConfigResponse.of(savedConfig, false, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesforceConfigResponse> getConfigurations() {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        return configRepository.findByOrganizationId(orgId).stream()
                .map(config -> {
                    Optional<IntegrationCredential> cred = credentialRepository
                            .findBySystemIdAndIntegrationType(config.getSystem().getId(), IntegrationType.SALESFORCE);
                    return SalesforceConfigResponse.of(
                            config,
                            cred.isPresent(),
                            cred.map(IntegrationCredential::getInstanceUrl).orElse(null),
                            cred.map(IntegrationCredential::getExternalOrgId).orElse(null)
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SalesforceConfigResponse getConfigurationById(UUID id) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        SalesforceConfiguration config = configRepository.findByIdAndOrganizationId(id, orgId)
                .or(() -> configRepository.findBySystemIdAndOrganizationId(id, orgId))
                .orElseThrow(() -> new ResourceNotFoundException("SalesforceConfiguration", "id", id));

        Optional<IntegrationCredential> cred = credentialRepository
                .findBySystemIdAndIntegrationType(config.getSystem().getId(), IntegrationType.SALESFORCE);

        return SalesforceConfigResponse.of(
                config,
                cred.isPresent(),
                cred.map(IntegrationCredential::getInstanceUrl).orElse(null),
                cred.map(IntegrationCredential::getExternalOrgId).orElse(null)
        );
    }

    @Override
    @Transactional
    public SalesforceTestResultDto testSalesforceConnection(UUID systemId) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        ConnectedSystem system = systemRepository.findByIdAndOrganizationId(systemId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConnectedSystem", "id", systemId));

        IntegrationCredential cred = credentialRepository
                .findBySystemIdAndIntegrationType(system.getId(), IntegrationType.SALESFORCE)
                .orElse(null);

        if (cred == null || cred.getEncryptedAccessToken() == null) {
            return new SalesforceTestResultDto(false, null, null, null, List.of(),
                    "Salesforce organization is not connected", "Missing OAuth credentials");
        }

        try {
            String accessToken = getValidAccessToken(cred, false);
            SalesforceLimitsDto limits = salesforceClient.getApiLimits(cred.getInstanceUrl(), accessToken, apiVersion);
            List<SalesforceApexJobDto> failedJobs = salesforceClient.getFailedApexJobs(cred.getInstanceUrl(), accessToken, apiVersion);

            return new SalesforceTestResultDto(
                    true,
                    cred.getInstanceUrl(),
                    cred.getExternalOrgId(),
                    limits,
                    failedJobs,
                    "Salesforce connection active. API Limits and Apex queries validated successfully.",
                    null
            );
        } catch (Exception e) {
            log.error("Salesforce connection test failed for system {}: {}", systemId, e.getMessage());
            return new SalesforceTestResultDto(false, cred.getInstanceUrl(), cred.getExternalOrgId(), null, List.of(),
                    "Connection check failed", e.getMessage());
        }
    }

    @Override
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void executeScheduledMonitoring() {
        List<SalesforceConfiguration> configs = configRepository.findAllActiveConfigurations();
        for (SalesforceConfiguration config : configs) {
            try {
                monitorConfiguration(config);
            } catch (Exception e) {
                log.error("Failed scheduled Salesforce monitoring for config {}: {}", config.getId(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void monitorConfiguration(SalesforceConfiguration config) {
        ConnectedSystem system = config.getSystem();
        IntegrationCredential cred = credentialRepository
                .findBySystemIdAndIntegrationType(system.getId(), IntegrationType.SALESFORCE)
                .orElse(null);

        if (cred == null || cred.getEncryptedAccessToken() == null) {
            return;
        }

        try {
            String accessToken = getValidAccessToken(cred, config.isSandbox());

            // A. Monitor API Usage Limits
            SalesforceLimitsDto limits = salesforceClient.getApiLimits(cred.getInstanceUrl(), accessToken, apiVersion);
            if (limits.percentageUsed() >= config.getApiUsageCriticalThresholdPct()) {
                Event event = new Event(
                        null,
                        config.getOrganization(),
                        system,
                        EventSourceType.SALESFORCE,
                        EventType.SALESFORCE_API_LIMIT_CRITICAL,
                        EventSeverity.CRITICAL,
                        String.format("Salesforce API usage critical: %.1f%% used (%d of %d)",
                                limits.percentageUsed(), (limits.max() - limits.remaining()), limits.max()),
                        null,
                        null,
                        Instant.now()
                );
                ingestionService.processNormalizedEvent(event, null, null, limits.percentageUsed());
            } else if (limits.percentageUsed() >= config.getApiUsageWarningThresholdPct()) {
                Event event = new Event(
                        null,
                        config.getOrganization(),
                        system,
                        EventSourceType.SALESFORCE,
                        EventType.SALESFORCE_API_LIMIT_WARNING,
                        EventSeverity.MEDIUM,
                        String.format("Salesforce API usage warning: %.1f%% used (%d of %d)",
                                limits.percentageUsed(), (limits.max() - limits.remaining()), limits.max()),
                        null,
                        null,
                        Instant.now()
                );
                ingestionService.processNormalizedEvent(event, null, null, limits.percentageUsed());
            }

            // B. Monitor Apex Jobs if enabled
            if (config.isMonitorApexJobs()) {
                List<SalesforceApexJobDto> failedJobs = salesforceClient
                        .getFailedApexJobs(cred.getInstanceUrl(), accessToken, apiVersion);

                if (!failedJobs.isEmpty()) {
                    SalesforceApexJobDto job = failedJobs.get(0);
                    Event event = new Event(
                            null,
                            config.getOrganization(),
                            system,
                            EventSourceType.SALESFORCE,
                            EventType.APEX_JOB_FAILED,
                            EventSeverity.HIGH,
                            String.format("Async Apex Job failed [%s]: %s (Errors: %d)",
                                    job.jobType(), job.extendedStatus() != null ? job.extendedStatus() : job.status(), job.numberOfErrors()),
                            null,
                            null,
                            job.createdDate()
                    );
                    ingestionService.processNormalizedEvent(event, null, null, null);
                }
                config.setLastApexJobCheckedAt(Instant.now());
            }

            system.setLastMonitoredAt(Instant.now());
            system.setStatus(SystemStatus.HEALTHY);
            systemRepository.save(system);
            configRepository.save(config);

        } catch (Exception e) {
            log.error("Salesforce monitoring execution failed for system {}: {}", system.getName(), e.getMessage());
            Event errEvent = new Event(
                    null,
                    config.getOrganization(),
                    system,
                    EventSourceType.SALESFORCE,
                    EventType.SALESFORCE_INTEGRATION_FAILURE,
                    EventSeverity.HIGH,
                    "Salesforce monitoring connectivity failure: " + e.getMessage(),
                    null,
                    null,
                    Instant.now()
            );
            ingestionService.processNormalizedEvent(errEvent, null, null, null);
        }
    }

    private String getValidAccessToken(IntegrationCredential cred, boolean isSandbox) throws Exception {
        String accessToken = encryptionUtil.decrypt(cred.getEncryptedAccessToken());

        // Check if access token needs refresh by attempting a quick probe or if refresh token exists
        if (cred.getEncryptedRefreshToken() != null) {
            try {
                // Quick validation probe
                salesforceClient.getApiLimits(cred.getInstanceUrl(), accessToken, apiVersion);
                return accessToken;
            } catch (Exception e) {
                log.info("Access token expired or rejected, initiating OAuth refresh flow for system {}", cred.getSystem().getId());
                String baseLoginUrl = isSandbox ? sandboxLoginUrl : prodLoginUrl;
                String tokenEndpoint = baseLoginUrl.replaceAll("/+$", "") + "/services/oauth2/token";
                String refreshToken = encryptionUtil.decrypt(cred.getEncryptedRefreshToken());

                JsonNode refreshResponse = salesforceClient.refreshToken(tokenEndpoint, clientId, clientSecret, refreshToken);
                String newAccessToken = refreshResponse.path("access_token").asText();
                String newInstanceUrl = refreshResponse.path("instance_url").asText(cred.getInstanceUrl());

                cred.setEncryptedAccessToken(encryptionUtil.encrypt(newAccessToken));
                cred.setInstanceUrl(newInstanceUrl);
                credentialRepository.save(cred);
                return newAccessToken;
            }
        }
        return accessToken;
    }
}
