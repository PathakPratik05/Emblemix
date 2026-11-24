package com.emblemix.event.service;

import com.emblemix.event.Event;
import com.emblemix.event.dto.EventIngestRequest;
import com.emblemix.event.dto.EventResponse;

import java.util.UUID;

/**
 * Service interface for ingesting raw operational telemetry, normalizing events, evaluating alert rules, and dispatching incidents.
 */
public interface EventIngestionService {

    /**
     * Ingests a new external event request, validates the system, and passes through the normalization pipeline.
     */
    EventResponse ingestEvent(UUID orgId, EventIngestRequest request);

    /**
     * Processes a normalized event against alert rules, handles recovery logic, updates system health, and triggers incident management.
     */
    Event processNormalizedEvent(Event rawEvent, Integer consecutiveFailures, Long responseTimeMs, Double apiUsagePct);
}
