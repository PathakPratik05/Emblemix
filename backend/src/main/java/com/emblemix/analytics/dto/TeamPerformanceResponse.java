package com.emblemix.analytics.dto;

import com.emblemix.team.TeamType;
import java.util.UUID;

public record TeamPerformanceResponse(
        UUID teamId,
        String teamName,
        TeamType teamType,
        long assignedIncidents,
        long resolvedIncidents,
        long openIncidents,
        long mttaMinutes,
        long mttrMinutes,
        double resolutionRatePercentage,
        double slaCompliancePercentage
) {}
