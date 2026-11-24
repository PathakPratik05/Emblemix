package com.emblemix.alert.dto;

import com.emblemix.event.EventSeverity;
import com.emblemix.event.EventType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AlertRuleRequest(
        UUID systemId,

        @NotBlank(message = "Rule name is required")
        @Size(max = 255)
        String name,

        @NotNull(message = "Event type is required")
        EventType eventType,

        @NotNull(message = "Target severity is required")
        EventSeverity targetSeverity,

        @Min(value = 1, message = "Failure count threshold must be at least 1")
        Integer failureCountThreshold,

        Long responseTimeThresholdMs,

        Double apiUsagePctThreshold,

        Boolean enabled
) {}
