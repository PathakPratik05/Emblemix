package com.emblemix.monitoring.salesforce.service;

import com.emblemix.monitoring.salesforce.SalesforceConfiguration;
import com.emblemix.monitoring.salesforce.dto.SalesforceConfigRequest;
import com.emblemix.monitoring.salesforce.dto.SalesforceConfigResponse;
import com.emblemix.monitoring.salesforce.dto.SalesforceTestResultDto;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Salesforce organization health monitoring, API limits, and Apex job checks.
 */
public interface SalesforceMonitoringService {

    /**
     * Registers a new Salesforce organization configuration and backing connected system.
     */
    SalesforceConfigResponse createSalesforceSystem(SalesforceConfigRequest request);

    /**
     * Lists registered Salesforce organizations and connection states for current tenant.
     */
    List<SalesforceConfigResponse> getConfigurations();

    /**
     * Retrieves a Salesforce configuration by ID or system ID.
     */
    SalesforceConfigResponse getConfigurationById(UUID id);

    /**
     * Tests live connectivity, queries API limits, and verifies Apex job query permissions.
     */
    SalesforceTestResultDto testSalesforceConnection(UUID systemId);

    /**
     * Background scheduler executing periodic Salesforce health checks.
     */
    void executeScheduledMonitoring();

    /**
     * Executes monitoring probes for a specific Salesforce organization configuration.
     */
    void monitorConfiguration(SalesforceConfiguration config);
}
