package com.emblemix.analytics;

import com.emblemix.analytics.dto.*;
import com.emblemix.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Governed Analytics & Power BI", description = "Standardized KPI calculation and Power BI reporting interfaces")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/kpis")
    @Operation(summary = "Get governed system and incident KPIs (Uptime, Availability, MTTA, MTTR, SLA achievement)")
    public ResponseEntity<ApiResponse<KpiSummaryResponse>> getKpis() {
        return ResponseEntity.ok(ApiResponse.success("KPIs calculated successfully", analyticsService.getKpiSummary()));
    }

    @GetMapping("/teams/performance")
    @Operation(summary = "Get team response, velocity, and SLA compliance metrics")
    public ResponseEntity<ApiResponse<List<TeamPerformanceResponse>>> getTeamPerformance() {
        return ResponseEntity.ok(ApiResponse.success("Team performance retrieved successfully", analyticsService.getTeamPerformance()));
    }

    @GetMapping("/governed/incidents")
    @Operation(summary = "Export governed incident dataset for Power BI and external analytics ingestion")
    public ResponseEntity<ApiResponse<List<GovernedIncidentRow>>> getGovernedIncidents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID systemId,
            @RequestParam(required = false) UUID teamId) {
        return ResponseEntity.ok(ApiResponse.success("Governed incidents retrieved", analyticsService.getGovernedIncidents(from, to, systemId, teamId)));
    }

    @GetMapping("/governed/monitoring")
    @Operation(summary = "Export governed monitoring dataset for Power BI and telemetry analytics")
    public ResponseEntity<ApiResponse<List<GovernedMonitoringRow>>> getGovernedMonitoring(
            @RequestParam(required = false) UUID systemId) {
        return ResponseEntity.ok(ApiResponse.success("Governed monitoring metrics retrieved", analyticsService.getGovernedMonitoring(systemId)));
    }

    @GetMapping("/powerbi/catalog")
    @Operation(summary = "List available enterprise Power BI reports and dataset refresh states")
    public ResponseEntity<ApiResponse<List<PowerBiReportCatalogItem>>> getPowerBiCatalog() {
        return ResponseEntity.ok(ApiResponse.success("Power BI catalog retrieved", analyticsService.getPowerBiCatalog()));
    }

    @GetMapping("/powerbi/embed-config/{reportId}")
    @Operation(summary = "Generate secure server-side Power BI embed configuration with organization data isolation")
    public ResponseEntity<ApiResponse<PowerBiEmbedConfigResponse>> getPowerBiEmbedConfig(
            @PathVariable String reportId) {
        return ResponseEntity.ok(ApiResponse.success("Power BI embed config generated", analyticsService.getPowerBiEmbedConfig(reportId)));
    }
}
