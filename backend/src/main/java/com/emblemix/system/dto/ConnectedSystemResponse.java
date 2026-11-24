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
        Instant lastMonitoredAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static ConnectedSystemResponse from(ConnectedSystem system) {
        return new ConnectedSystemResponse(
                system.getId(),
                system.getOrganization().getId(),
                system.getName(),
                system.getSystemType(),
                system.getDescription(),
                system.getEnvironment(),
                system.getStatus(),
                system.isEnabled(),
                system.getLastMonitoredAt(),
                system.getCreatedAt(),
                system.getUpdatedAt()
        );
    }
}
