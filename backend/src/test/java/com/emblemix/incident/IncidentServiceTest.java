package com.emblemix.incident;

import com.emblemix.event.Event;
import com.emblemix.event.EventSeverity;
import com.emblemix.event.EventSourceType;
import com.emblemix.event.EventType;
import com.emblemix.notification.NotificationService;
import com.emblemix.organization.Organization;
import com.emblemix.system.ConnectedSystem;
import com.emblemix.system.SystemType;
import com.emblemix.team.TeamRepository;
import com.emblemix.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentEventRepository incidentEventRepository;

    @Mock
    private IncidentHistoryRepository historyRepository;

    @Mock
    private IncidentCommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private NotificationService notificationService;

    private IncidentService incidentService;

    private Organization org;
    private ConnectedSystem system;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(
                incidentRepository,
                incidentEventRepository,
                historyRepository,
                commentRepository,
                userRepository,
                teamRepository,
                notificationService
        );
        org = new Organization(UUID.randomUUID(), "Acme", "acme");
        system = new ConnectedSystem(UUID.randomUUID(), org, "API Gateway", SystemType.WEBSITE, null, "PRODUCTION");
    }

    @Test
    @DisplayName("Should create new incident when no open incident exists for fingerprint")
    void testCreateNewIncident() {
        Event event = new Event(
                UUID.randomUUID(), org, system, EventSourceType.WEBSITE,
                EventType.WEBSITE_DOWN, EventSeverity.CRITICAL, "Website Down", null, "fp-123", null
        );

        when(incidentRepository.findFirstByOrganizationIdAndFingerprintAndStatusIn(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        Incident incident = incidentService.createOrGroupIncident(event, IncidentSeverity.CRITICAL);

        assertNotNull(incident);
        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        assertEquals(IncidentSeverity.CRITICAL, incident.getSeverity());
        assertEquals("fp-123", incident.getFingerprint());

        verify(notificationService).dispatchIncidentNotifications(any(Incident.class), eq("CREATED"));
        verify(incidentEventRepository).save(any(IncidentEvent.class));
    }

    @Test
    @DisplayName("Should group duplicate event into existing open incident without creating duplicate incident")
    void testGroupDuplicateEventIntoExistingIncident() {
        Event event = new Event(
                UUID.randomUUID(), org, system, EventSourceType.WEBSITE,
                EventType.WEBSITE_DOWN, EventSeverity.HIGH, "Website Down Again", null, "fp-123", null
        );

        Incident existingIncident = new Incident(
                UUID.randomUUID(), org, system, "Existing Incident", "Desc",
                IncidentSeverity.HIGH, null, "fp-123"
        );

        when(incidentRepository.findFirstByOrganizationIdAndFingerprintAndStatusIn(any(), any(), any()))
                .thenReturn(Optional.of(existingIncident));
        when(incidentEventRepository.existsByIncidentIdAndEventId(any(), any())).thenReturn(false);

        Incident incident = incidentService.createOrGroupIncident(event, IncidentSeverity.HIGH);

        assertSame(existingIncident, incident);
        // Verify incident was NOT saved as a new incident
        verify(incidentRepository, never()).save(any(Incident.class));
        verify(incidentEventRepository).save(any(IncidentEvent.class));
        verify(notificationService, never()).dispatchIncidentNotifications(any(), eq("CREATED"));
    }

    @Test
    @DisplayName("Should auto-resolve existing open incidents matching recovery fingerprint")
    void testAutoResolveByFingerprint() {
        Incident existing = new Incident(
                UUID.randomUUID(), org, system, "Down", "Desc",
                IncidentSeverity.CRITICAL, null, "fp-123"
        );
        existing.setStatus(IncidentStatus.OPEN);

        when(incidentRepository.findByOrganizationIdAndFingerprintAndStatusIn(any(), any(), any()))
                .thenReturn(List.of(existing));

        incidentService.autoResolveByFingerprint(org.getId(), "fp-123", "Website recovered");

        assertEquals(IncidentStatus.RESOLVED, existing.getStatus());
        assertNotNull(existing.getResolvedAt());
        verify(incidentRepository).save(existing);
        verify(notificationService).dispatchIncidentNotifications(existing, "RESOLVED");
    }

    @Test
    @DisplayName("Should automatically route new incident to system's owning team and primary owner")
    void testIntelligentRoutingToTeamAndOwner() {
        com.emblemix.team.Team devopsTeam = new com.emblemix.team.Team(
                UUID.randomUUID(), org, "DevOps Team", com.emblemix.team.TeamType.DEVOPS, "Infra", null
        );
        com.emblemix.user.User owner = new com.emblemix.user.User(
                UUID.randomUUID(), org, "owner@acme.com", "hash", "Bob", "Owner", com.emblemix.user.Role.ENGINEER
        );
        system.setOwningTeam(devopsTeam);
        system.setPrimaryOwner(owner);

        Event event = new Event(
                UUID.randomUUID(), org, system, EventSourceType.WEBSITE,
                EventType.WEBSITE_DOWN, EventSeverity.CRITICAL, "Server Down", null, "fp-route-test", null
        );

        when(incidentRepository.findFirstByOrganizationIdAndFingerprintAndStatusIn(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        Incident created = incidentService.createOrGroupIncident(event, IncidentSeverity.CRITICAL);

        assertNotNull(created);
        assertNotNull(created.getAssignedTeam());
        assertEquals("DevOps Team", created.getAssignedTeam().getName());
        assertNotNull(created.getAssignedUser());
        assertEquals("owner@acme.com", created.getAssignedUser().getEmail());
        verify(historyRepository, atLeast(2)).save(any(IncidentHistory.class));
    }

    @Test
    @DisplayName("Should escalate incident and elevate severity")
    void testEscalateIncident() {
        UUID incidentId = UUID.randomUUID();
        Incident incident = new Incident(
                incidentId, org, system, "High Latency", "Desc",
                IncidentSeverity.MEDIUM, null, "fp-escalate"
        );
        incident.setStatus(IncidentStatus.OPEN);

        com.emblemix.common.security.TenantContext.setCurrentTenant(org.getId());
        try {
            when(incidentRepository.findByIdAndOrganizationId(incidentId, org.getId()))
                    .thenReturn(Optional.of(incident));
            when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

            var response = incidentService.escalateIncident(incidentId, "SLA approaching breach");

            assertEquals(1, response.escalationLevel());
            assertEquals(IncidentSeverity.HIGH, response.severity());
            assertNotNull(response.escalatedAt());
            verify(notificationService).dispatchIncidentNotifications(any(Incident.class), eq("ESCALATED"));
        } finally {
            com.emblemix.common.security.TenantContext.clear();
        }
    }
}
