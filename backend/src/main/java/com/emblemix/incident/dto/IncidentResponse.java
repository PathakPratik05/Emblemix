package com.emblemix.incident.dto;

import com.emblemix.incident.Incident;
import com.emblemix.incident.IncidentSeverity;
import com.emblemix.incident.IncidentStatus;

import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        UUID organizationId,
        UUID systemId,
        String systemName,
        String title,
        String description,
        IncidentSeverity severity,
        IncidentStatus status,
        String fingerprint,
        UUID assignedTeamId,
        String assignedTeamName,
        UUID assignedUserId,
        String assignedUserName,
        int escalationLevel,
        Instant escalatedAt,
        String resolutionNotes,
        Instant acknowledgedAt,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static IncidentResponse from(Incident incident) {
        String assignedName = null;
        if (incident.getAssignedUser() != null) {
            assignedName = incident.getAssignedUser().getFirstName() + " " + incident.getAssignedUser().getLastName();
        }
        String teamName = incident.getAssignedTeam() != null ? incident.getAssignedTeam().getName() : null;
        UUID teamId = incident.getAssignedTeam() != null ? incident.getAssignedTeam().getId() : null;

        return new IncidentResponse(
                incident.getId(),
                incident.getOrganization().getId(),
                incident.getSystem().getId(),
                incident.getSystem().getName(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getFingerprint(),
                teamId,
                teamName,
                incident.getAssignedUser() != null ? incident.getAssignedUser().getId() : null,
                assignedName,
                incident.getEscalationLevel(),
                incident.getEscalatedAt(),
                incident.getResolutionNotes(),
                incident.getAcknowledgedAt(),
                incident.getResolvedAt(),
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }
}
