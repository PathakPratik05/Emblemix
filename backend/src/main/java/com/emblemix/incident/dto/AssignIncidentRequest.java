package com.emblemix.incident.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignIncidentRequest(
        @NotNull(message = "Assigned user ID is required")
        UUID userId
) {}
