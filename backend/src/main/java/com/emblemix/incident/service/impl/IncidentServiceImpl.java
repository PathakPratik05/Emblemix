package com.emblemix.incident.service.impl;

import com.emblemix.common.exception.BadRequestException;
import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.common.response.PagedResponse;
import com.emblemix.common.security.SecurityUtils;
import com.emblemix.event.Event;
import com.emblemix.incident.*;
import com.emblemix.incident.dto.*;
import com.emblemix.incident.service.IncidentService;
import com.emblemix.notification.service.NotificationService;
import com.emblemix.system.ConnectedSystem;
import com.emblemix.team.Team;
import com.emblemix.team.TeamRepository;
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
public class IncidentServiceImpl implements IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentServiceImpl.class);

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
    private final TeamRepository teamRepository;
    private final NotificationService notificationService;

    public IncidentServiceImpl(IncidentRepository incidentRepository,
                               IncidentEventRepository incidentEventRepository,
                               IncidentHistoryRepository historyRepository,
                               IncidentCommentRepository commentRepository,
                               UserRepository userRepository,
                               TeamRepository teamRepository,
                               NotificationService notificationService) {
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.historyRepository = historyRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.notificationService = notificationService;
    }

    @Override
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

        // Intelligent routing based on connected system ownership
        ConnectedSystem system = event.getSystem();
        if (system != null) {
            if (system.getOwningTeam() != null) {
                newIncident.setAssignedTeam(system.getOwningTeam());
            }
            if (system.getPrimaryOwner() != null) {
                newIncident.setAssignedUser(system.getPrimaryOwner());
            }
        }

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

        if (savedIncident.getAssignedTeam() != null) {
            historyRepository.save(new IncidentHistory(
                    null,
                    savedIncident,
                    null,
                    "ROUTED_TO_TEAM",
                    null,
                    savedIncident.getAssignedTeam().getName(),
                    "Automatically routed to owning team '" + savedIncident.getAssignedTeam().getName() + "' based on system configuration"
            ));
        }

        if (savedIncident.getAssignedUser() != null) {
            historyRepository.save(new IncidentHistory(
                    null,
                    savedIncident,
                    null,
                    "ROUTED_TO_OWNER",
                    null,
                    savedIncident.getAssignedUser().getFirstName() + " " + savedIncident.getAssignedUser().getLastName(),
                    "Assigned to primary system owner " + savedIncident.getAssignedUser().getEmail()
            ));
        }

        // Dispatch notifications
        notificationService.dispatchIncidentNotifications(savedIncident, "CREATED");

        log.info("Created new incident {} with severity {}", savedIncident.getId(), savedIncident.getSeverity());
        return savedIncident;
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<IncidentResponse> getIncidents(IncidentStatus status, IncidentSeverity severity, Pageable pageable) {
        return getIncidents(status, severity, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<IncidentResponse> getIncidents(IncidentStatus status, IncidentSeverity severity, UUID teamId, Pageable pageable) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Page<Incident> page;
        if (teamId != null) {
            page = incidentRepository.findByOrganizationIdAndAssignedTeamId(orgId, teamId, pageable);
        } else if (status != null) {
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    @Transactional
    public IncidentResponse assignTeam(UUID id, UUID teamId) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Incident incident = incidentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));

        Team team = teamRepository.findByIdAndOrganizationId(teamId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        User currentUser = null;
        var principalOpt = SecurityUtils.getCurrentUserPrincipal();
        if (principalOpt.isPresent()) {
            currentUser = userRepository.findById(principalOpt.get().getId()).orElse(null);
        }

        String prevTeamName = incident.getAssignedTeam() != null ? incident.getAssignedTeam().getName() : "None";
        incident.setAssignedTeam(team);
        Incident saved = incidentRepository.save(incident);

        historyRepository.save(new IncidentHistory(
                null,
                saved,
                currentUser,
                "TEAM_ASSIGNED",
                prevTeamName,
                team.getName(),
                "Reassigned to team " + team.getName()
        ));

        notificationService.dispatchIncidentNotifications(saved, "TEAM_ASSIGNED");
        return IncidentResponse.from(saved);
    }

    @Override
    @Transactional
    public IncidentResponse escalateIncident(UUID id, String reason) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Incident incident = incidentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));

        if (incident.getStatus() == IncidentStatus.RESOLVED || incident.getStatus() == IncidentStatus.CLOSED) {
            throw new BadRequestException("Cannot escalate an already resolved or closed incident");
        }

        User currentUser = null;
        var principalOpt = SecurityUtils.getCurrentUserPrincipal();
        if (principalOpt.isPresent()) {
            currentUser = userRepository.findById(principalOpt.get().getId()).orElse(null);
        }

        int prevLevel = incident.getEscalationLevel();
        incident.setEscalationLevel(prevLevel + 1);
        incident.setEscalatedAt(Instant.now());

        IncidentSeverity oldSeverity = incident.getSeverity();
        if (oldSeverity == IncidentSeverity.LOW) {
            incident.setSeverity(IncidentSeverity.MEDIUM);
        } else if (oldSeverity == IncidentSeverity.MEDIUM) {
            incident.setSeverity(IncidentSeverity.HIGH);
        } else if (oldSeverity == IncidentSeverity.HIGH) {
            incident.setSeverity(IncidentSeverity.CRITICAL);
        }

        Incident saved = incidentRepository.save(incident);

        historyRepository.save(new IncidentHistory(
                null,
                saved,
                currentUser,
                "ESCALATED",
                "Level " + prevLevel,
                "Level " + saved.getEscalationLevel(),
                "Escalated (Severity: " + oldSeverity + " -> " + saved.getSeverity() + "). Reason: " +
                        (reason != null && !reason.isBlank() ? reason.trim() : "Manual escalation policy triggered")
        ));

        notificationService.dispatchIncidentNotifications(saved, "ESCALATED");
        return IncidentResponse.from(saved);
    }

    @Override
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

    @Override
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
