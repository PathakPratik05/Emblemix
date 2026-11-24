package com.emblemix.system.dto;

import com.emblemix.system.ConnectedSystem;
import com.emblemix.system.SystemStatus;
import com.emblemix.system.SystemType;

import java.time.Instant;
import java.util.UUID;

public record ConnectedSystemResponse(
        UUID id,
        UUID organizationId,
        String name,
        SystemType systemType,
        String description,
        String environment,
        SystemStatus status,
        boolean enabled,
        UUID owningTeamId,
        String owningTeamName,
        UUID primaryOwnerId,
        String primaryOwnerName,
        UUID backupOwnerId,
        String backupOwnerName,
        String criticality,
        String escalationPolicy,
        Instant lastMonitoredAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static ConnectedSystemResponse from(ConnectedSystem system) {
        String teamName = system.getOwningTeam() != null ? system.getOwningTeam().getName() : null;
        UUID teamId = system.getOwningTeam() != null ? system.getOwningTeam().getId() : null;

        String primaryName = system.getPrimaryOwner() != null
                ? system.getPrimaryOwner().getFirstName() + " " + system.getPrimaryOwner().getLastName()
                : null;
        UUID primaryId = system.getPrimaryOwner() != null ? system.getPrimaryOwner().getId() : null;

        String backupName = system.getBackupOwner() != null
                ? system.getBackupOwner().getFirstName() + " " + system.getBackupOwner().getLastName()
                : null;
        UUID backupId = system.getBackupOwner() != null ? system.getBackupOwner().getId() : null;

        return new ConnectedSystemResponse(
                system.getId(),
                system.getOrganization().getId(),
                system.getName(),
                system.getSystemType(),
                system.getDescription(),
                system.getEnvironment(),
                system.getStatus(),
                system.isEnabled(),
                teamId,
                teamName,
                primaryId,
                primaryName,
                backupId,
                backupName,
                system.getCriticality(),
                system.getEscalationPolicy(),
                system.getLastMonitoredAt(),
                system.getCreatedAt(),
                system.getUpdatedAt()
        );
    }
}
