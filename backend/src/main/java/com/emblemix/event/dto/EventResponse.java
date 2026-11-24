package com.emblemix.event.dto;

import com.emblemix.event.Event;
import com.emblemix.event.EventSeverity;
import com.emblemix.event.EventSourceType;
import com.emblemix.event.EventStatus;
import com.emblemix.event.EventType;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        UUID organizationId,
        UUID systemId,
        String systemName,
        EventSourceType sourceType,
        EventType eventType,
        EventSeverity severity,
        String message,
        String detailsJson,
        String fingerprint,
        EventStatus status,
        Instant occurredAt,
        Instant receivedAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getOrganization().getId(),
                event.getSystem().getId(),
                event.getSystem().getName(),
                event.getSourceType(),
                event.getEventType(),
                event.getSeverity(),
                event.getMessage(),
                event.getDetailsJson(),
                event.getFingerprint(),
                event.getStatus(),
                event.getOccurredAt(),
                event.getReceivedAt()
        );
    }
}
