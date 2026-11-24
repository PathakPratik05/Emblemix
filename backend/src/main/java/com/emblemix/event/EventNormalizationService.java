package com.emblemix.event;

import com.emblemix.common.util.FingerprintUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class EventNormalizationService {

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
