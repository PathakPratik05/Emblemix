package com.emblemix.reporting.service;

import com.emblemix.reporting.dto.DashboardOverviewDto;
import com.emblemix.reporting.dto.IncidentTrendDto;
import com.emblemix.reporting.dto.UptimeReportDto;

import java.util.List;

/**
 * Service interface for operational dashboard metrics, incident trends, uptime rollups, and CSV exports.
 */
public interface ReportingService {

    /**
     * Compiles an organization-wide overview of systems, incident counts, uptime percentages, and average resolution times.
     */
    DashboardOverviewDto getDashboardOverview();

    /**
     * Calculates rolling incident creation and resolution volumes over the specified number of days.
     */
    List<IncidentTrendDto> getIncidentTrends(int days);

    /**
     * Aggregates availability and latency reports per connected system over the specified time window.
     */
    List<UptimeReportDto> getUptimeReports(int days);

    /**
     * Exports tabular incident history as an RFC-4180 compliant CSV string.
     */
    String exportIncidentsCsv();
}
