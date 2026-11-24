package com.emblemix.incident.service;

import com.emblemix.common.response.PagedResponse;
import com.emblemix.event.Event;
import com.emblemix.incident.Incident;
import com.emblemix.incident.IncidentSeverity;
import com.emblemix.incident.IncidentStatus;
import com.emblemix.incident.dto.IncidentCommentDto;
import com.emblemix.incident.dto.IncidentDetailResponse;
import com.emblemix.incident.dto.IncidentResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for automated incident creation, deduplication, intelligent team routing,
 * acknowledgment, escalation, resolution, and engineering comments.
 */
public interface IncidentService {

    /**
     * Creates a new incident or groups incoming failure event into an existing active incident matching fingerprint.
     */
    Incident createOrGroupIncident(Event event, IncidentSeverity ruleSeverity);

    /**
     * Automatically resolves open incidents matching fingerprint upon receiving a system recovery signal.
     */
    void autoResolveByFingerprint(UUID orgId, String fingerprint, String resolutionNote);

    /**
     * Queries incidents within the tenant filtered by status and severity.
     */
    PagedResponse<IncidentResponse> getIncidents(IncidentStatus status, IncidentSeverity severity, Pageable pageable);

    /**
     * Queries incidents within the tenant filtered by status, severity, and owning team ID.
     */
    PagedResponse<IncidentResponse> getIncidents(IncidentStatus status, IncidentSeverity severity, UUID teamId, Pageable pageable);

    /**
     * Retrieves full incident details, audit timeline, and comments.
     */
    IncidentDetailResponse getIncidentDetails(UUID id);

    /**
     * Marks an open incident as acknowledged by the current responder.
     */
    IncidentResponse acknowledgeIncident(UUID id);

    /**
     * Assigns or reassigns an individual engineer to an incident.
     */
    IncidentResponse assignIncident(UUID id, UUID userId);

    /**
     * Assigns or reassigns an engineering team to own an incident.
     */
    IncidentResponse assignTeam(UUID id, UUID teamId);

    /**
     * Escalates an incident to the next tier level, bumping severity and recording audit logs.
     */
    IncidentResponse escalateIncident(UUID id, String reason);

    /**
     * Marks an incident as resolved, recording resolution notes and triggering notifications.
     */
    IncidentResponse resolveIncident(UUID id, String resolutionNotes);

    /**
     * Appends an investigation note or update to the incident discussion thread.
     */
    IncidentCommentDto addComment(UUID id, String commentText);
}
