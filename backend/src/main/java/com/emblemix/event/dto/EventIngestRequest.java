package com.emblemix.event.dto;

import com.emblemix.event.EventSeverity;
import com.emblemix.event.EventSourceType;
import com.emblemix.event.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record EventIngestRequest(
        @NotNull(message = "System ID is required")
        UUID systemId,

        @NotNull(message = "Source type is required")
        EventSourceType sourceType,

        @NotNull(message = "Event type is required")
        EventType eventType,

        @NotNull(message = "Severity is required")
        EventSeverity severity,

        @NotBlank(message = "Message is required")
        String message,

        String detailsJson,

        Instant occurredAt
) {}
