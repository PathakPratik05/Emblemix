package com.emblemix.system.dto;

import com.emblemix.system.SystemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConnectedSystemRequest(
        @NotBlank(message = "System name is required")
        @Size(max = 255)
        String name,

        @NotNull(message = "System type is required")
        SystemType systemType,

        String description,

        String environment,

        Boolean enabled,

        java.util.UUID owningTeamId,

        java.util.UUID primaryOwnerId,

        java.util.UUID backupOwnerId,

        String criticality,

        String escalationPolicy
) {}
