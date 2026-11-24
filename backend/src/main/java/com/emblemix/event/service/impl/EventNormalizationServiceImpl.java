package com.emblemix.event.service.impl;

import com.emblemix.common.util.FingerprintUtil;
import com.emblemix.event.Event;
import com.emblemix.event.EventSourceType;
import com.emblemix.event.EventType;
import com.emblemix.event.service.EventNormalizationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class EventNormalizationServiceImpl implements EventNormalizationService {

    @Override
    public String generateFingerprint(UUID organizationId, UUID systemId, EventSourceType sourceType, EventType eventType) {
        String baseType = eventType.name();
        // Group recovery events with their failure counterpart
        if (eventType == EventType.WEBSITE_RECOVERED) {
            baseType = EventType.WEBSITE_DOWN.name();
        } else if (eventType == EventType.WEBHOOK_RECOVERED) {
            baseType = EventType.WEBHOOK_INCIDENT.name();
        }
        return FingerprintUtil.generateFingerprint(
                organizationId.toString(),
                systemId.toString(),
                sourceType.name(),
                baseType
        );
    }

    @Override
    public Event normalize(Event event) {
        if (event.getFingerprint() == null || event.getFingerprint().isBlank()) {
            event.setFingerprint(generateFingerprint(
                    event.getOrganization().getId(),
                    event.getSystem().getId(),
                    event.getSourceType(),
                    event.getEventType()
            ));
        }
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(Instant.now());
        }
        return event;
    }
}
