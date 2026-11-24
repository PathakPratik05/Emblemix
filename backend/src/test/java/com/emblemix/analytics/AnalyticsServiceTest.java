package com.emblemix.analytics;

import com.emblemix.analytics.dto.KpiSummaryResponse;
import com.emblemix.analytics.dto.PowerBiEmbedConfigResponse;
import com.emblemix.analytics.dto.TeamPerformanceResponse;
import com.emblemix.analytics.service.AnalyticsService;
import com.emblemix.analytics.service.impl.AnalyticsServiceImpl;
import com.emblemix.common.security.TenantContext;
import com.emblemix.incident.Incident;
import com.emblemix.incident.IncidentRepository;
import com.emblemix.incident.IncidentSeverity;
import com.emblemix.incident.IncidentStatus;
import com.emblemix.organization.Organization;
import com.emblemix.system.ConnectedSystem;
import com.emblemix.system.ConnectedSystemRepository;
import com.emblemix.system.SystemStatus;
import com.emblemix.system.SystemType;
import com.emblemix.team.Team;
import com.emblemix.team.TeamRepository;
import com.emblemix.team.TeamType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ConnectedSystemRepository systemRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private TeamRepository teamRepository;

    private AnalyticsService analyticsService;

    private UUID orgId;
    private Organization org;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsServiceImpl(systemRepository, incidentRepository, teamRepository);
        orgId = UUID.randomUUID();
        TenantContext.setCurrentTenant(orgId);
        org = new Organization(orgId, "Acme Corp", "acme-corp");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should accurately calculate governed KPIs including MTTA, MTTR, and SLA achievement")
    void testGetKpiSummary() {
        ConnectedSystem sys1 = new ConnectedSystem(UUID.randomUUID(), org, "Web App", SystemType.WEBSITE, null, "PRODUCTION");
        sys1.setStatus(SystemStatus.HEALTHY);
        when(systemRepository.findByOrganizationId(orgId)).thenReturn(List.of(sys1));

        Instant now = Instant.now();
        Instant created1 = now.minus(60, ChronoUnit.MINUTES);
        Instant ack1 = created1.plus(10, ChronoUnit.MINUTES); // 10m MTTA, met 15m critical SLA
        Instant res1 = created1.plus(30, ChronoUnit.MINUTES); // 30m MTTR

        Incident inc1 = new Incident(UUID.randomUUID(), org, sys1, "DB Crash", "Desc", IncidentSeverity.CRITICAL, null, "fp-1");
        inc1.setCreatedAt(created1);
        inc1.setAcknowledgedAt(ack1);
        inc1.setResolvedAt(res1);
        inc1.setStatus(IncidentStatus.RESOLVED);

        when(incidentRepository.findByOrganizationId(orgId)).thenReturn(List.of(inc1));

        KpiSummaryResponse kpis = analyticsService.getKpiSummary();

        assertNotNull(kpis);
        assertEquals(1, kpis.totalSystems());
        assertEquals(1, kpis.healthySystems());
        assertEquals(100.0, kpis.systemHealthPercentage());
        assertEquals(1, kpis.totalIncidents());
        assertEquals(1, kpis.criticalIncidents());
        assertEquals(1, kpis.resolvedIncidents());
        assertEquals(10, kpis.mttaMinutes());
        assertEquals(30, kpis.mttrMinutes());
        assertEquals(100.0, kpis.slaAchievementPercentage());
    }

    @Test
    @DisplayName("Should calculate team performance breakdown across incidents")
    void testGetTeamPerformance() {
        Team devops = new Team(UUID.randomUUID(), org, "DevOps", TeamType.DEVOPS, "Infra", null);
        when(teamRepository.findByOrganizationId(orgId)).thenReturn(List.of(devops));

        ConnectedSystem sys = new ConnectedSystem(UUID.randomUUID(), org, "K8s", SystemType.INFRASTRUCTURE, null, "PRODUCTION");
        Instant now = Instant.now();
        Incident inc = new Incident(UUID.randomUUID(), org, sys, "Node failure", "Desc", IncidentSeverity.HIGH, null, "fp-node");
        inc.setCreatedAt(now.minus(40, ChronoUnit.MINUTES));
        inc.setAcknowledgedAt(now.minus(25, ChronoUnit.MINUTES)); // 15m MTTA
        inc.setResolvedAt(now.minus(10, ChronoUnit.MINUTES));     // 30m MTTR
        inc.setStatus(IncidentStatus.RESOLVED);
        inc.setAssignedTeam(devops);

        when(incidentRepository.findByOrganizationIdAndAssignedTeamId(orgId, devops.getId()))
                .thenReturn(List.of(inc));

        List<TeamPerformanceResponse> performance = analyticsService.getTeamPerformance();

        assertNotNull(performance);
        assertEquals(1, performance.size());
        TeamPerformanceResponse teamPerf = performance.get(0);
        assertEquals("DevOps", teamPerf.teamName());
        assertEquals(1, teamPerf.assignedIncidents());
        assertEquals(1, teamPerf.resolvedIncidents());
        assertEquals(0, teamPerf.openIncidents());
        assertEquals(15, teamPerf.mttaMinutes());
        assertEquals(30, teamPerf.mttrMinutes());
        assertEquals(100.0, teamPerf.resolutionRatePercentage());
    }

    @Test
    @DisplayName("Should generate secure Power BI embed configuration with organization isolation claim")
    void testGetPowerBiEmbedConfig() {
        PowerBiEmbedConfigResponse config = analyticsService.getPowerBiEmbedConfig("pbi-rep-001");

        assertNotNull(config);
        assertEquals("pbi-rep-001", config.reportId());
        assertNotNull(config.embedUrl());
        assertTrue(config.embedUrl().contains("pbi-rep-001"));
        assertNotNull(config.embedToken());
        assertEquals(orgId.toString(), config.organizationId());
        assertNotNull(config.tokenExpiration());
    }
}
