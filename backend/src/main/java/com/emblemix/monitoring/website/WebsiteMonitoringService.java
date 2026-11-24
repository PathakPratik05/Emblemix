package com.emblemix.monitoring.website;

import com.emblemix.common.exception.BadRequestException;
import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.common.response.PagedResponse;
import com.emblemix.common.security.SecurityUtils;
import com.emblemix.event.*;
import com.emblemix.monitoring.website.dto.*;
import com.emblemix.organization.Organization;
import com.emblemix.organization.OrganizationRepository;
import com.emblemix.system.ConnectedSystem;
import com.emblemix.system.ConnectedSystemRepository;
import com.emblemix.system.SystemStatus;
import com.emblemix.system.SystemType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebsiteMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(WebsiteMonitoringService.class);

    private final WebsiteConfigurationRepository configRepository;
    private final MonitoringResultRepository resultRepository;
    private final ConnectedSystemRepository systemRepository;
    private final OrganizationRepository organizationRepository;
    private final WebsiteCheckExecutor checkExecutor;
    private final EventIngestionService ingestionService;

    // Concurrency guard to prevent overlapping checks for the same target
    private final Set<UUID> activeCheckJobs = ConcurrentHashMap.newKeySet();

    public WebsiteMonitoringService(WebsiteConfigurationRepository configRepository,
                                    MonitoringResultRepository resultRepository,
                                    ConnectedSystemRepository systemRepository,
                                    OrganizationRepository organizationRepository,
                                    WebsiteCheckExecutor checkExecutor,
                                    EventIngestionService ingestionService) {
        this.configRepository = configRepository;
        this.resultRepository = resultRepository;
        this.systemRepository = systemRepository;
        this.organizationRepository = organizationRepository;
        this.checkExecutor = checkExecutor;
        this.ingestionService = ingestionService;
    }

    @Transactional
    public WebsiteConfigResponse createWebsite(WebsiteConfigRequest request) {
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
                SystemType.WEBSITE,
                request.description(),
                request.environment() != null ? request.environment() : "PRODUCTION"
        );
        ConnectedSystem savedSystem = systemRepository.save(system);

        WebsiteConfiguration config = new WebsiteConfiguration(
                UUID.randomUUID(),
                savedSystem,
                org,
                request.url().trim(),
                request.monitoringIntervalSeconds() != null ? request.monitoringIntervalSeconds() : 60,
                request.requestTimeoutMs() != null ? request.requestTimeoutMs() : 5000,
                request.expectedHttpStatusMin() != null ? request.expectedHttpStatusMin() : 200,
                request.expectedHttpStatusMax() != null ? request.expectedHttpStatusMax() : 299,
                request.expectedResponseContent(),
                request.checkSsl() != null ? request.checkSsl() : true,
                request.sslExpiryThresholdDays() != null ? request.sslExpiryThresholdDays() : 30,
                request.consecutiveFailureThreshold() != null ? request.consecutiveFailureThreshold() : 2
        );
        WebsiteConfiguration savedConfig = configRepository.save(config);

        return WebsiteConfigResponse.from(savedConfig);
    }

    @Transactional(readOnly = true)
    public PagedResponse<WebsiteConfigResponse> getWebsites(Pageable pageable) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Page<WebsiteConfiguration> page = configRepository.findByOrganizationId(orgId, pageable);
        return new PagedResponse<>(
                page.map(WebsiteConfigResponse::from).getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public WebsiteConfigResponse getWebsiteById(UUID id) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        WebsiteConfiguration config = configRepository.findByIdAndOrganizationId(id, orgId)
                .or(() -> configRepository.findBySystemIdAndOrganizationId(id, orgId))
                .orElseThrow(() -> new ResourceNotFoundException("WebsiteConfiguration", "id", id));
        return WebsiteConfigResponse.from(config);
    }

    @Transactional
    public WebsiteConfigResponse updateWebsite(UUID id, WebsiteConfigRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        WebsiteConfiguration config = configRepository.findByIdAndOrganizationId(id, orgId)
                .or(() -> configRepository.findBySystemIdAndOrganizationId(id, orgId))
                .orElseThrow(() -> new ResourceNotFoundException("WebsiteConfiguration", "id", id));

        ConnectedSystem system = config.getSystem();
        system.setName(request.name().trim());
        system.setDescription(request.description());
        if (request.environment() != null) {
            system.setEnvironment(request.environment());
        }
        systemRepository.save(system);

        config.setUrl(request.url().trim());
        if (request.monitoringIntervalSeconds() != null) {
            config.setMonitoringIntervalSeconds(request.monitoringIntervalSeconds());
        }
        if (request.requestTimeoutMs() != null) {
            config.setRequestTimeoutMs(request.requestTimeoutMs());
        }
        if (request.expectedHttpStatusMin() != null) {
            config.setExpectedHttpStatusMin(request.expectedHttpStatusMin());
        }
        if (request.expectedHttpStatusMax() != null) {
            config.setExpectedHttpStatusMax(request.expectedHttpStatusMax());
        }
        config.setExpectedResponseContent(request.expectedResponseContent());
        if (request.checkSsl() != null) {
            config.setCheckSsl(request.checkSsl());
        }
        if (request.sslExpiryThresholdDays() != null) {
            config.setSslExpiryThresholdDays(request.sslExpiryThresholdDays());
        }
        if (request.consecutiveFailureThreshold() != null) {
            config.setConsecutiveFailureThreshold(request.consecutiveFailureThreshold());
        }

        return WebsiteConfigResponse.from(configRepository.save(config));
    }

    @Transactional(readOnly = true)
    public WebsiteTestResultDto testWebsite(UUID id) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        WebsiteConfiguration config = configRepository.findByIdAndOrganizationId(id, orgId)
                .or(() -> configRepository.findBySystemIdAndOrganizationId(id, orgId))
                .orElseThrow(() -> new ResourceNotFoundException("WebsiteConfiguration", "id", id));

        return checkExecutor.executeCheck(
                config.getUrl(),
                config.getRequestTimeoutMs(),
                config.getExpectedHttpStatusMin(),
                config.getExpectedHttpStatusMax(),
                config.getExpectedResponseContent(),
                config.isCheckSsl(),
                config.getSslExpiryThresholdDays()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<MonitoringResultDto> getMonitoringHistory(UUID systemId, Pageable pageable) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Page<MonitoringResult> page = resultRepository
                .findBySystemIdAndOrganizationIdOrderByCheckedAtDesc(systemId, orgId, pageable);

        return new PagedResponse<>(
                page.map(MonitoringResultDto::from).getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Scheduled(fixedDelay = 20000)
    public void executeScheduledChecks() {
        List<WebsiteConfiguration> configs = configRepository.findAllActiveConfigurations();
        for (WebsiteConfiguration config : configs) {
            if (!activeCheckJobs.add(config.getId())) {
                continue; // check already running for this target
            }
            try {
                runCheckForConfig(config);
            } catch (Exception e) {
                log.error("Error executing monitoring check for config {}: {}", config.getId(), e.getMessage());
            } finally {
                activeCheckJobs.remove(config.getId());
            }
        }
    }

    @Transactional
    public void runCheckForConfig(WebsiteConfiguration config) {
        WebsiteTestResultDto testResult = checkExecutor.executeCheck(
                config.getUrl(),
                config.getRequestTimeoutMs(),
                config.getExpectedHttpStatusMin(),
                config.getExpectedHttpStatusMax(),
                config.getExpectedResponseContent(),
                config.isCheckSsl(),
                config.getSslExpiryThresholdDays()
        );

        String status = testResult.healthy() ? "SUCCESS" : "FAILURE";
        MonitoringResult result = new MonitoringResult(
                null,
                config.getOrganization(),
                config.getSystem(),
                status,
                testResult.httpStatusCode(),
                testResult.responseTimeMs(),
                testResult.sslValid(),
                testResult.sslDaysRemaining(),
                testResult.error(),
                null
        );
        resultRepository.save(result);

        ConnectedSystem system = config.getSystem();
        system.setLastMonitoredAt(Instant.now());

        if (testResult.healthy()) {
            if (config.getCurrentConsecutiveFailures() > 0) {
                // Recovery event detected!
                Event recoveryEvent = new Event(
                        null,
                        config.getOrganization(),
                        system,
                        EventSourceType.WEBSITE,
                        EventType.WEBSITE_RECOVERED,
                        EventSeverity.LOW,
                        "Website recovered and reachable: " + config.getUrl(),
                        null,
                        null,
                        Instant.now()
                );
                ingestionService.processNormalizedEvent(recoveryEvent, 0, testResult.responseTimeMs(), null);
                config.setCurrentConsecutiveFailures(0);
            } else {
                system.setStatus(SystemStatus.HEALTHY);
                systemRepository.save(system);
            }
        } else {
            int failures = config.getCurrentConsecutiveFailures() + 1;
            config.setCurrentConsecutiveFailures(failures);

            if (failures >= config.getConsecutiveFailureThreshold()) {
                // Determine appropriate event type
                EventType eventType = EventType.WEBSITE_DOWN;
                EventSeverity severity = EventSeverity.CRITICAL;

                if (testResult.httpStatusCode() != null && testResult.httpStatusCode() >= 500) {
                    eventType = EventType.HTTP_ERROR;
                    severity = EventSeverity.HIGH;
                }

                Event failureEvent = new Event(
                        null,
                        config.getOrganization(),
                        system,
                        EventSourceType.WEBSITE,
                        eventType,
                        severity,
                        "Website check failed: " + testResult.message(),
                        testResult.error(),
                        null,
                        Instant.now()
                );
                ingestionService.processNormalizedEvent(failureEvent, failures, testResult.responseTimeMs(), null);
            }
        }
        configRepository.save(config);
    }
}
