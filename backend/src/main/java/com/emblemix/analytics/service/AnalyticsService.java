package com.emblemix.analytics.service;

import com.emblemix.analytics.dto.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for governed analytics, MTTA/MTTR calculations, team performance velocity,
 * tabular data export endpoints, and Power BI Embedded integration.
 */
public interface AnalyticsService {

    /**
     * Calculates organization-wide MTTA, MTTR, SLA achievement percentage, uptime, and availability.
     */
    KpiSummaryResponse getKpiSummary();

    /**
     * Calculates per-team MTTA, MTTR, active incident queues, and SLA compliance percentages.
     */
    List<TeamPerformanceResponse> getTeamPerformance();

    /**
     * Exports governed tabular incident records for BI ingestion and audit reporting.
     */
    List<GovernedIncidentRow> getGovernedIncidents(Instant from, Instant to, UUID systemId, UUID teamId);

    /**
     * Exports governed synthetic health check telemetry for BI ingestion.
     */
    List<GovernedMonitoringRow> getGovernedMonitoring(UUID systemId);

    /**
     * Lists provisioned Power BI reports available to the tenant organization.
     */
    List<PowerBiReportCatalogItem> getPowerBiCatalog();

    /**
     * Generates a server-side Power BI embed configuration with tenant-isolated token claims.
     */
    PowerBiEmbedConfigResponse getPowerBiEmbedConfig(String reportId);
}
