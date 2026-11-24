package com.emblemix.analytics.dto;

import com.emblemix.incident.IncidentSeverity;
import com.emblemix.incident.IncidentStatus;
import com.emblemix.system.SystemType;
import java.time.Instant;
import java.util.UUID;

public record GovernedIncidentRow(
        UUID incidentId,
        UUID organizationId,
        UUID systemId,
        String systemName,
        SystemType systemType,
        String environment,
        String criticality,
        UUID teamId,
        String teamName,
        IncidentSeverity severity,
        IncidentStatus status,
        String fingerprint,
        Instant createdAt,
        Instant acknowledgedAt,
        Instant resolvedAt,
        Long mttaMinutes,
        Long mttrMinutes,
        boolean slaMet,
        int escalationLevel
) {}
