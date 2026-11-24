package com.emblemix.incident;

import com.emblemix.common.exception.BadRequestException;
import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.common.response.PagedResponse;
import com.emblemix.common.security.SecurityUtils;
import com.emblemix.event.Event;
import com.emblemix.incident.dto.*;
import com.emblemix.notification.NotificationService;
import com.emblemix.user.User;
import com.emblemix.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private static final Set<IncidentStatus> ACTIVE_STATUSES = Set.of(
            IncidentStatus.OPEN,
            IncidentStatus.ACKNOWLEDGED,
            IncidentStatus.INVESTIGATING
    );

    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final IncidentHistoryRepository historyRepository;
    private final IncidentCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public IncidentService(IncidentRepository incidentRepository,
                           IncidentEventRepository incidentEventRepository,
                           IncidentHistoryRepository historyRepository,
                           IncidentCommentRepository commentRepository,
                           UserRepository userRepository,
                           NotificationService notificationService) {
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.historyRepository = historyRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Incident createOrGroupIncident(Event event, IncidentSeverity ruleSeverity) {
        IncidentSeverity severity = ruleSeverity != null ? ruleSeverity : mapEventSeverity(event.getSeverity());

        // Check if an unresolved incident with the same fingerprint already exists
        Optional<Incident> existingIncidentOpt = incidentRepository
                .findFirstByOrganizationIdAndFingerprintAndStatusIn(
                        event.getOrganization().getId(),
                        event.getFingerprint(),
                        ACTIVE_STATUSES
                );

        if (existingIncidentOpt.isPresent()) {
            Incident incident = existingIncidentOpt.get();
            // Link event to existing incident
            if (!incidentEventRepository.existsByIncidentIdAndEventId(incident.getId(), event.getId())) {
                incidentEventRepository.save(new IncidentEvent(null, incident, event));
            }

            // Escalate severity if new event is higher
            if (severity.ordinal() > incident.getSeverity().ordinal()) {
                IncidentSeverity oldSeverity = incident.getSeverity();
                incident.setSeverity(severity);
                incidentRepository.save(incident);

                historyRepository.save(new IncidentHistory(
                        null,
                        incident,
                        null,
                        "SEVERITY_ESCALATED",
                        oldSeverity.name(),
                        severity.name(),
                        "Severity automatically escalated due to repeated failure event: " + event.getMessage()
                ));
            }

            log.info("Grouped event {} into existing incident {}", event.getId(), incident.getId());
            return incident;
        }

        // Create new incident
        Incident newIncident = new Incident(
                UUID.randomUUID(),
                event.getOrganization(),
                event.getSystem(),
                "Incident: " + event.getMessage(),
                event.getMessage(),
                severity,
                event,
                event.getFingerprint()
        );
        Incident savedIncident = incidentRepository.save(newIncident);

        incidentEventRepository.save(new IncidentEvent(null, savedIncident, event));

        historyRepository.save(new IncidentHistory(
                null,
                savedIncident,
                null,
                "CREATED",
                null,
                IncidentStatus.OPEN.name(),
                "Incident automatically detected and created from source event: " + event.getEventType()
        ));

        // Dispatch notifications
        notificationService.dispatchIncidentNotifications(savedIncident, "CREATED");

        log.info("Created new incident {} with severity {}", savedIncident.getId(), savedIncident.getSeverity());
        return savedIncident;
    }

    @Transactional
    public void autoResolveByFingerprint(UUID orgId, String fingerprint, String resolutionNote) {
        List<Incident> activeIncidents = incidentRepository
                .findByOrganizationIdAndFingerprintAndStatusIn(orgId, fingerprint, ACTIVE_STATUSES);

        for (Incident incident : activeIncidents) {
            IncidentStatus prevStatus = incident.getStatus();
            incident.setStatus(IncidentStatus.RESOLVED);
            incident.setResolvedAt(Instant.now());
            incident.setResolutionNotes(resolutionNote != null ? resolutionNote : "Automatically resolved: recovery signal detected.");
            incidentRepository.save(incident);

            historyRepository.save(new IncidentHistory(
                    null,
                    incident,
                    null,
                    "AUTO_RESOLVED",
                    prevStatus.name(),
                    IncidentStatus.RESOLVED.name(),
                    "Automatically recovered: " + incident.getResolutionNotes()
            ));

            notificationService.dispatchIncidentNotifications(incident, "RESOLVED");
            log.info("Auto-resolved incident {} on system {}", incident.getId(), incident.getSystem().getName());
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<IncidentResponse> getIncidents(IncidentStatus status, IncidentSeverity severity, Pageable pageable) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Page<Incident> page;
        if (status != null) {
            page = incidentRepository.findByOrganizationIdAndStatus(orgId, status, pageable);
        } else if (severity != null) {
            page = incidentRepository.findByOrganizationIdAndSeverity(orgId, severity, pageable);
        } else {
            page = incidentRepository.findByOrganizationId(orgId, pageable);
        }

        return new PagedResponse<>(
                page.map(IncidentResponse::from).getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public IncidentDetailResponse getIncidentDetails(UUID id) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Incident incident = incidentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));

        List<IncidentHistoryDto> timeline = historyRepository.findByIncidentIdOrderByCreatedAtAsc(id).stream()
                .map(IncidentHistoryDto::from)
                .collect(Collectors.toList());

        List<IncidentCommentDto> comments = commentRepository.findByIncidentIdOrderByCreatedAtAsc(id).stream()
                .map(IncidentCommentDto::from)
                .collect(Collectors.toList());

        return IncidentDetailResponse.of(incident, timeline, comments);
    }

    @Transactional
    public IncidentResponse acknowledgeIncident(UUID id) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Incident incident = incidentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));

        if (incident.getStatus() == IncidentStatus.RESOLVED || incident.getStatus() == IncidentStatus.CLOSED) {
            throw new BadRequestException("Cannot acknowledge an already resolved or closed incident");
        }

        User user = null;
        var principalOpt = SecurityUtils.getCurrentUserPrincipal();
        if (principalOpt.isPresent()) {
            user = userRepository.findById(principalOpt.get().getId()).orElse(null);
        }

        IncidentStatus prevStatus = incident.getStatus();
        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        incident.setAcknowledgedAt(Instant.now());
        if (user != null && incident.getAssignedUser() == null) {
            incident.setAssignedUser(user);
        }
        Incident saved = incidentRepository.save(incident);

        historyRepository.save(new IncidentHistory(
                null,
                saved,
                user,
                "ACKNOWLEDGED",
                prevStatus.name(),
                IncidentStatus.ACKNOWLEDGED.name(),
                user != null ? "Acknowledged by " + user.getFirstName() + " " + user.getLastName() : "Acknowledged"
        ));

        notificationService.dispatchIncidentNotifications(saved, "ACKNOWLEDGED");
        return IncidentResponse.from(saved);
    }

    @Transactional
    public IncidentResponse assignIncident(UUID id, UUID userId) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Incident incident = incidentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));

        User assignee = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!assignee.getOrganization().getId().equals(orgId)) {
            throw new BadRequestException("Cannot assign incident to a user outside your organization");
        }

        User currentUser = null;
        var principalOpt = SecurityUtils.getCurrentUserPrincipal();
        if (principalOpt.isPresent()) {
            currentUser = userRepository.findById(principalOpt.get().getId()).orElse(null);
        }

        incident.setAssignedUser(assignee);
        Incident saved = incidentRepository.save(incident);

        historyRepository.save(new IncidentHistory(
                null,
                saved,
                currentUser,
                "ASSIGNED",
                null,
                null,
                "Assigned to " + assignee.getFirstName() + " " + assignee.getLastName()
        ));

        return IncidentResponse.from(saved);
    }

    @Transactional
    public IncidentResponse resolveIncident(UUID id, String resolutionNotes) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Incident incident = incidentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));

        User currentUser = null;
        var principalOpt = SecurityUtils.getCurrentUserPrincipal();
        if (principalOpt.isPresent()) {
            currentUser = userRepository.findById(principalOpt.get().getId()).orElse(null);
        }

        IncidentStatus prevStatus = incident.getStatus();
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(Instant.now());
        incident.setResolutionNotes(resolutionNotes != null ? resolutionNotes.trim() : "Resolved manually");
        Incident saved = incidentRepository.save(incident);

        historyRepository.save(new IncidentHistory(
                null,
                saved,
                currentUser,
                "RESOLVED",
                prevStatus.name(),
                IncidentStatus.RESOLVED.name(),
                "Incident resolved with notes: " + incident.getResolutionNotes()
        ));

        notificationService.dispatchIncidentNotifications(saved, "RESOLVED");
        return IncidentResponse.from(saved);
    }

    @Transactional
    public IncidentCommentDto addComment(UUID id, String commentText) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Incident incident = incidentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));

        User currentUser = SecurityUtils.getCurrentUserPrincipal()
                .flatMap(p -> userRepository.findById(p.getId()))
                .orElseThrow(() -> new BadRequestException("Authenticated user required to add comments"));

        IncidentComment comment = new IncidentComment(null, incident, currentUser, commentText.trim());
        IncidentComment saved = commentRepository.save(comment);

        historyRepository.save(new IncidentHistory(
                null,
                incident,
                currentUser,
                "COMMENT_ADDED",
                null,
                null,
                "Added comment: " + commentText.trim()
        ));

        return IncidentCommentDto.from(saved);
    }

    private IncidentSeverity mapEventSeverity(com.emblemix.event.EventSeverity eventSeverity) {
        return switch (eventSeverity) {
            case CRITICAL -> IncidentSeverity.CRITICAL;
            case HIGH -> IncidentSeverity.HIGH;
            case MEDIUM -> IncidentSeverity.MEDIUM;
            case LOW -> IncidentSeverity.LOW;
        };
    }
}
