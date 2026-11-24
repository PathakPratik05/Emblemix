package com.emblemix.alert.dto;

import com.emblemix.alert.AlertRule;
import com.emblemix.event.EventSeverity;
import com.emblemix.event.EventType;

import java.time.Instant;
import java.util.UUID;

public record AlertRuleResponse(
        UUID id,
        UUID organizationId,
        UUID systemId,
        String systemName,
        String name,
        EventType eventType,
        EventSeverity targetSeverity,
        int failureCountThreshold,
        Long responseTimeThresholdMs,
        Double apiUsagePctThreshold,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public static AlertRuleResponse from(AlertRule rule) {
        return new AlertRuleResponse(
                rule.getId(),
                rule.getOrganization().getId(),
                rule.getSystem() != null ? rule.getSystem().getId() : null,
                rule.getSystem() != null ? rule.getSystem().getName() : "All Systems",
                rule.getName(),
                rule.getEventType(),
                rule.getTargetSeverity(),
                rule.getFailureCountThreshold(),
                rule.getResponseTimeThresholdMs(),
                rule.getApiUsagePctThreshold(),
                rule.isEnabled(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}
