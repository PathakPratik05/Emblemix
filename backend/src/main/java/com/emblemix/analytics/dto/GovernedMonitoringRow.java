package com.emblemix.analytics.dto;

import com.emblemix.system.SystemStatus;
import com.emblemix.system.SystemType;
import java.time.Instant;
import java.util.UUID;

public record GovernedMonitoringRow(
        UUID systemId,
        String systemName,
        SystemType systemType,
        String environment,
        SystemStatus status,
        Integer responseTimeMs,
        Double uptimePercentage,
        Instant timestamp
) {}
