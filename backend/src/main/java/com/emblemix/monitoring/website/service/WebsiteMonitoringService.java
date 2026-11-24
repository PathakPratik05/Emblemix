package com.emblemix.monitoring.website.service;

import com.emblemix.common.response.PagedResponse;
import com.emblemix.monitoring.website.WebsiteConfiguration;
import com.emblemix.monitoring.website.dto.MonitoringResultDto;
import com.emblemix.monitoring.website.dto.WebsiteConfigRequest;
import com.emblemix.monitoring.website.dto.WebsiteConfigResponse;
import com.emblemix.monitoring.website.dto.WebsiteTestResultDto;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for synthetic HTTP/HTTPS website health checks, SSL certificate monitoring, and history.
 */
public interface WebsiteMonitoringService {

    /**
     * Registers a new website monitoring configuration and backing connected system.
     */
    WebsiteConfigResponse createWebsite(WebsiteConfigRequest request);

    /**
     * Lists registered website configurations for the tenant.
     */
    PagedResponse<WebsiteConfigResponse> getWebsites(Pageable pageable);

    /**
     * Retrieves website configuration by ID or system ID.
     */
    WebsiteConfigResponse getWebsiteById(UUID id);

    /**
     * Updates an existing website monitoring configuration and URL parameters.
     */
    WebsiteConfigResponse updateWebsite(UUID id, WebsiteConfigRequest request);

    /**
     * Performs an immediate on-demand probe of a website endpoint.
     */
    WebsiteTestResultDto testWebsite(UUID id);

    /**
     * Queries historical health check results for a monitored website system.
     */
    PagedResponse<MonitoringResultDto> getMonitoringHistory(UUID systemId, Pageable pageable);

    /**
     * Background scheduler executing periodic synthetic checks for active website configurations.
     */
    void executeScheduledChecks();

    /**
     * Executes a single probe iteration for a specific website configuration.
     */
    void runCheckForConfig(WebsiteConfiguration config);
}
