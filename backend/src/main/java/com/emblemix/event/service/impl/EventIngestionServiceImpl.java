package com.emblemix.event.service.impl;

import com.emblemix.alert.AlertRuleEngine;
import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.event.*;
import com.emblemix.event.dto.EventIngestRequest;
import com.emblemix.event.dto.EventResponse;
import com.emblemix.event.service.EventIngestionService;
import com.emblemix.event.service.EventNormalizationService;
import com.emblemix.incident.IncidentSeverity;
import com.emblemix.incident.service.IncidentService;
import com.emblemix.organization.Organization;
import com.emblemix.system.ConnectedSystem;
import com.emblemix.system.ConnectedSystemRepository;
import com.emblemix.system.SystemStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class EventIngestionServiceImpl implements EventIngestionService {

    private static final Logger log = LoggerFactory.getLogger(EventIngestionServiceImpl.class);

    private final EventRepository eventRepository;
    private final ConnectedSystemRepository systemRepository;
    private final EventNormalizationService normalizationService;
    private final AlertRuleEngine alertRuleEngine;
    private final IncidentService incidentService;

    public EventIngestionServiceImpl(EventRepository eventRepository,
                                     ConnectedSystemRepository systemRepository,
                                     EventNormalizationService normalizationService,
                                     AlertRuleEngine alertRuleEngine,
                                     IncidentService incidentService) {
        this.eventRepository = eventRepository;
        this.systemRepository = systemRepository;
        this.normalizationService = normalizationService;
        this.alertRuleEngine = alertRuleEngine;
        this.incidentService = incidentService;
    }

    @Override
    @Transactional
    public EventResponse ingestEvent(UUID orgId, EventIngestRequest request) {
        ConnectedSystem system = systemRepository.findByIdAndOrganizationId(request.systemId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConnectedSystem", "id", request.systemId()));

        Event event = new Event(
                null,
                system.getOrganization(),
                system,
                request.sourceType(),
                request.eventType(),
                request.severity(),
                request.message(),
                request.detailsJson(),
                null,
                request.occurredAt()
        );

        return EventResponse.from(processNormalizedEvent(event, null, null, null));
    }

    @Override
    @Transactional
    public Event processNormalizedEvent(Event rawEvent, Integer consecutiveFailures, Long responseTimeMs, Double apiUsagePct) {
        Event event = normalizationService.normalize(rawEvent);

        if (event.getEventType() == EventType.WEBSITE_RECOVERED || event.getEventType() == EventType.WEBHOOK_RECOVERED) {
            // Recovery event: auto-resolve incidents for this system and fingerprint
            event.setStatus(EventStatus.PROCESSED);
            Event saved = eventRepository.save(event);

            incidentService.autoResolveByFingerprint(
                    event.getOrganization().getId(),
                    event.getFingerprint(),
                    "System recovered: " + event.getMessage()
            );

            // Update system status
            ConnectedSystem sys = event.getSystem();
            sys.setStatus(SystemStatus.HEALTHY);
            sys.setLastMonitoredAt(Instant.now());
            systemRepository.save(sys);

            log.info("Recovery event processed for system: {}", sys.getName());
            return saved;
        }

        // Failure or degradation event
        Optional<EventSeverity> ruleSeverity = alertRuleEngine.evaluateRules(
                event, consecutiveFailures, responseTimeMs, apiUsagePct
        );

        event.setStatus(EventStatus.PROCESSED);
        Event saved = eventRepository.save(event);

        IncidentSeverity targetSeverity = ruleSeverity
                .map(this::mapToIncidentSeverity)
                .orElseGet(() -> mapToIncidentSeverity(event.getSeverity()));

        incidentService.createOrGroupIncident(saved, targetSeverity);

        // Update system status accordingly
        ConnectedSystem sys = event.getSystem();
        if (targetSeverity == IncidentSeverity.CRITICAL) {
            sys.setStatus(SystemStatus.DOWN);
        } else if (targetSeverity == IncidentSeverity.HIGH || targetSeverity == IncidentSeverity.MEDIUM) {
            sys.setStatus(SystemStatus.DEGRADED);
        }
        sys.setLastMonitoredAt(Instant.now());
        systemRepository.save(sys);

        return saved;
    }

    private IncidentSeverity mapToIncidentSeverity(EventSeverity sev) {
        return switch (sev) {
            case CRITICAL -> IncidentSeverity.CRITICAL;
            case HIGH -> IncidentSeverity.HIGH;
            case MEDIUM -> IncidentSeverity.MEDIUM;
            case LOW -> IncidentSeverity.LOW;
        };
    }
}
