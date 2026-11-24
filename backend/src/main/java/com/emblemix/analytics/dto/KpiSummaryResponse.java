package com.emblemix.analytics.dto;

import java.time.Instant;

public record KpiSummaryResponse(
        double uptimePercentage,
        double availabilityPercentage,
        long mttaMinutes,
        long mttrMinutes,
        long totalIncidents,
        long criticalIncidents,
        long resolvedIncidents,
        long openIncidents,
        long incidentRecurrenceCount,
        double slaAchievementPercentage,
        long totalSystems,
        long healthySystems,
        double systemHealthPercentage,
        double integrationHealthPercentage,
        Instant calculatedAt
) {}
