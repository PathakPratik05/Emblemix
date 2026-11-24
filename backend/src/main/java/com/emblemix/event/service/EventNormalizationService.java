package com.emblemix.event.service;

import com.emblemix.event.Event;
import com.emblemix.event.EventSourceType;
import com.emblemix.event.EventType;

import java.util.UUID;

/**
 * Service interface for normalizing operational events and generating deterministic deduplication fingerprints.
 */
public interface EventNormalizationService {

    /**
     * Generates a deterministic SHA-256 fingerprint for event grouping and deduplication.
     */
    String generateFingerprint(UUID organizationId, UUID systemId, EventSourceType sourceType, EventType eventType);

    /**
     * Normalizes timestamp, fingerprint, and payload consistency before event evaluation.
     */
    Event normalize(Event event);
}
