package com.emblemix.incident.dto;

import jakarta.validation.constraints.NotBlank;

public record ResolveIncidentRequest(
        @NotBlank(message = "Resolution notes are required")
        String resolutionNotes
) {}
