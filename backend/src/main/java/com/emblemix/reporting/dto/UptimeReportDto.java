package com.emblemix.reporting.dto;

import java.util.UUID;

public record UptimeReportDto(
        UUID systemId,
        String systemName,
        double uptimePercentage,
        double averageResponseTimeMs,
        long totalChecks,
        long successfulChecks,
        long failedChecks
) {}
