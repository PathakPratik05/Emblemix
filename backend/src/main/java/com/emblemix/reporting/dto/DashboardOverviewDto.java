package com.emblemix.reporting.dto;

import java.util.Map;

public record DashboardOverviewDto(
        long totalSystems,
        long healthySystems,
        long degradedSystems,
        long downSystems,
        long openIncidents,
        long criticalIncidents,
        long resolvedTodayIncidents,
        double averageResolutionTimeMinutes,
        double websiteUptimePercentage,
        double averageResponseTimeMs,
        Map<String, Long> systemHealthDistribution,
        Map<String, Long> incidentSeverityDistribution
) {}
