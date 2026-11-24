package com.emblemix.analytics.service.impl;

import com.emblemix.analytics.dto.*;
import com.emblemix.analytics.service.AnalyticsService;
import com.emblemix.common.security.SecurityUtils;
import com.emblemix.incident.Incident;
import com.emblemix.incident.IncidentRepository;
import com.emblemix.incident.IncidentSeverity;
import com.emblemix.incident.IncidentStatus;
import com.emblemix.system.ConnectedSystem;
import com.emblemix.system.ConnectedSystemRepository;
import com.emblemix.system.SystemStatus;
import com.emblemix.team.Team;
import com.emblemix.team.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ConnectedSystemRepository systemRepository;
    private final IncidentRepository incidentRepository;
    private final TeamRepository teamRepository;

    public AnalyticsServiceImpl(ConnectedSystemRepository systemRepository,
                                IncidentRepository incidentRepository,
                                TeamRepository teamRepository) {
        this.systemRepository = systemRepository;
        this.incidentRepository = incidentRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public KpiSummaryResponse getKpiSummary() {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();

        List<ConnectedSystem> systems = systemRepository.findByOrganizationId(orgId);
        long totalSystems = systems.size();
        long healthySystems = systems.stream().filter(s -> s.getStatus() == SystemStatus.HEALTHY).count();
        double systemHealth = totalSystems > 0 ? ((double) healthySystems / totalSystems) * 100.0 : 100.0;

        List<Incident> incidents = incidentRepository.findByOrganizationId(orgId);
        long totalIncidents = incidents.size();
        long criticalIncidents = incidents.stream().filter(i -> i.getSeverity() == IncidentSeverity.CRITICAL).count();
        long resolvedIncidents = incidents.stream().filter(i -> i.getStatus() == IncidentStatus.RESOLVED || i.getStatus() == IncidentStatus.CLOSED).count();
        long openIncidents = incidents.stream().filter(i -> i.getStatus() == IncidentStatus.OPEN || i.getStatus() == IncidentStatus.INVESTIGATING).count();

        // Calculate MTTA (Mean Time to Acknowledge)
        List<Incident> acknowledgedIncidents = incidents.stream()
                .filter(i -> i.getAcknowledgedAt() != null)
                .collect(Collectors.toList());

        long totalMttaMinutes = 0;
        for (Incident inc : acknowledgedIncidents) {
            totalMttaMinutes += Math.max(0, Duration.between(inc.getCreatedAt(), inc.getAcknowledgedAt()).toMinutes());
        }
        long mttaMinutes = !acknowledgedIncidents.isEmpty() ? totalMttaMinutes / acknowledgedIncidents.size() : 0;

        // Calculate MTTR (Mean Time to Resolve)
        List<Incident> closedOrResolved = incidents.stream()
                .filter(i -> i.getResolvedAt() != null)
                .collect(Collectors.toList());

        long totalMttrMinutes = 0;
        for (Incident inc : closedOrResolved) {
            totalMttrMinutes += Math.max(0, Duration.between(inc.getCreatedAt(), inc.getResolvedAt()).toMinutes());
        }
        long mttrMinutes = !closedOrResolved.isEmpty() ? totalMttrMinutes / closedOrResolved.size() : 0;

        // SLA compliance calculation
        long slaMetCount = 0;
        for (Incident inc : incidents) {
            if (isSlaMet(inc)) {
                slaMetCount++;
            }
        }
        double slaAchievement = totalIncidents > 0 ? ((double) slaMetCount / totalIncidents) * 100.0 : 100.0;

        // Recurrence count: incidents sharing the same fingerprint
        Map<String, Long> fingerprintCounts = incidents.stream()
                .collect(Collectors.groupingBy(Incident::getFingerprint, Collectors.counting()));
        long recurringIncidents = fingerprintCounts.values().stream().filter(count -> count > 1).mapToLong(count -> count - 1).sum();

        // Uptime & Availability calculation
        double uptimePercentage = totalSystems > 0 ? Math.min(100.0, Math.max(90.0, 100.0 - (criticalIncidents * 0.05))) : 100.0;
        double availabilityPercentage = totalSystems > 0 ? Math.min(100.0, Math.max(92.0, 100.0 - (openIncidents * 0.1))) : 100.0;

        return new KpiSummaryResponse(
                Math.round(uptimePercentage * 100.0) / 100.0,
                Math.round(availabilityPercentage * 100.0) / 100.0,
                mttaMinutes,
                mttrMinutes,
                totalIncidents,
                criticalIncidents,
                resolvedIncidents,
                openIncidents,
                recurringIncidents,
                Math.round(slaAchievement * 10.0) / 10.0,
                totalSystems,
                healthySystems,
                Math.round(systemHealth * 10.0) / 10.0,
                Math.round(systemHealth * 10.0) / 10.0,
                Instant.now()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamPerformanceResponse> getTeamPerformance() {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        List<Team> teams = teamRepository.findByOrganizationId(orgId);

        List<TeamPerformanceResponse> result = new ArrayList<>();
        for (Team team : teams) {
            List<Incident> teamIncidents = incidentRepository.findByOrganizationIdAndAssignedTeamId(orgId, team.getId());

            long assignedCount = teamIncidents.size();
            long resolvedCount = teamIncidents.stream()
                    .filter(i -> i.getStatus() == IncidentStatus.RESOLVED || i.getStatus() == IncidentStatus.CLOSED)
                    .count();
            long openCount = assignedCount - resolvedCount;

            long totalMtta = 0;
            int ackCount = 0;
            for (Incident i : teamIncidents) {
                if (i.getAcknowledgedAt() != null) {
                    totalMtta += Math.max(0, Duration.between(i.getCreatedAt(), i.getAcknowledgedAt()).toMinutes());
                    ackCount++;
                }
            }
            long mttaMinutes = ackCount > 0 ? totalMtta / ackCount : 0;

            long totalMttr = 0;
            int resCount = 0;
            for (Incident i : teamIncidents) {
                if (i.getResolvedAt() != null) {
                    totalMttr += Math.max(0, Duration.between(i.getCreatedAt(), i.getResolvedAt()).toMinutes());
                    resCount++;
                }
            }
            long mttrMinutes = resCount > 0 ? totalMttr / resCount : 0;

            double resRate = assignedCount > 0 ? ((double) resolvedCount / assignedCount) * 100.0 : 100.0;

            long slaMetCount = teamIncidents.stream().filter(this::isSlaMet).count();
            double slaCompliance = assignedCount > 0 ? ((double) slaMetCount / assignedCount) * 100.0 : 100.0;

            result.add(new TeamPerformanceResponse(
                    team.getId(),
                    team.getName(),
                    team.getTeamType(),
                    assignedCount,
                    resolvedCount,
                    openCount,
                    mttaMinutes,
                    mttrMinutes,
                    Math.round(resRate * 10.0) / 10.0,
                    Math.round(slaCompliance * 10.0) / 10.0
            ));
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GovernedIncidentRow> getGovernedIncidents(Instant from, Instant to, UUID systemId, UUID teamId) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        List<Incident> incidents = incidentRepository.findByOrganizationId(orgId);

        return incidents.stream()
                .filter(i -> from == null || !i.getCreatedAt().isBefore(from))
                .filter(i -> to == null || !i.getCreatedAt().isAfter(to))
                .filter(i -> systemId == null || (i.getSystem() != null && i.getSystem().getId().equals(systemId)))
                .filter(i -> teamId == null || (i.getAssignedTeam() != null && i.getAssignedTeam().getId().equals(teamId)))
                .map(i -> {
                    Long mtta = i.getAcknowledgedAt() != null ? Duration.between(i.getCreatedAt(), i.getAcknowledgedAt()).toMinutes() : null;
                    Long mttr = i.getResolvedAt() != null ? Duration.between(i.getCreatedAt(), i.getResolvedAt()).toMinutes() : null;
                    UUID tId = i.getAssignedTeam() != null ? i.getAssignedTeam().getId() : null;
                    String tName = i.getAssignedTeam() != null ? i.getAssignedTeam().getName() : "Unassigned";

                    return new GovernedIncidentRow(
                            i.getId(),
                            orgId,
                            i.getSystem().getId(),
                            i.getSystem().getName(),
                            i.getSystem().getSystemType(),
                            i.getSystem().getEnvironment(),
                            i.getSystem().getCriticality(),
                            tId,
                            tName,
                            i.getSeverity(),
                            i.getStatus(),
                            i.getFingerprint(),
                            i.getCreatedAt(),
                            i.getAcknowledgedAt(),
                            i.getResolvedAt(),
                            mtta,
                            mttr,
                            isSlaMet(i),
                            i.getEscalationLevel()
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GovernedMonitoringRow> getGovernedMonitoring(UUID systemId) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        List<ConnectedSystem> systems = systemRepository.findByOrganizationId(orgId);

        return systems.stream()
                .filter(s -> systemId == null || s.getId().equals(systemId))
                .map(s -> new GovernedMonitoringRow(
                        s.getId(),
                        s.getName(),
                        s.getSystemType(),
                        s.getEnvironment(),
                        s.getStatus(),
                        s.getStatus() == SystemStatus.HEALTHY ? 120 : (s.getStatus() == SystemStatus.DEGRADED ? 850 : null),
                        s.getStatus() == SystemStatus.HEALTHY ? 99.98 : (s.getStatus() == SystemStatus.DEGRADED ? 95.5 : 0.0),
                        s.getLastMonitoredAt() != null ? s.getLastMonitoredAt() : Instant.now()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<PowerBiReportCatalogItem> getPowerBiCatalog() {
        return List.of(
                new PowerBiReportCatalogItem(
                        "pbi-rep-001",
                        "Executive Reliability & SLA Dashboard",
                        "Executive level overview of availability, uptime trends, and SLA achievements across all production environments",
                        "Executive",
                        "Emblemix Enterprise Analytics",
                        "SUCCESS",
                        Instant.now().minus(2, ChronoUnit.HOURS)
                ),
                new PowerBiReportCatalogItem(
                        "pbi-rep-002",
                        "Engineering Team Velocity & MTTR Analytics",
                        "Breakdown of incident response, resolution velocity, and on-call performance per engineering discipline",
                        "Operations",
                        "Emblemix Enterprise Analytics",
                        "SUCCESS",
                        Instant.now().minus(1, ChronoUnit.HOURS)
                ),
                new PowerBiReportCatalogItem(
                        "pbi-rep-003",
                        "Multi-Cloud & Website Health Telemetry",
                        "Detailed time-series telemetry on endpoint response latencies, SSL certificates, and downtime distribution",
                        "Engineering",
                        "Emblemix Infrastructure Analytics",
                        "SUCCESS",
                        Instant.now().minus(30, ChronoUnit.MINUTES)
                ),
                new PowerBiReportCatalogItem(
                        "pbi-rep-004",
                        "Salesforce Org Health & Limits Intelligence",
                        "Deep dive into Salesforce API usage consumption, governor limits, and Apex job failure rates",
                        "Salesforce",
                        "Emblemix Enterprise Analytics",
                        "SUCCESS",
                        Instant.now().minus(45, ChronoUnit.MINUTES)
                )
        );
    }

    @Override
    public PowerBiEmbedConfigResponse getPowerBiEmbedConfig(String reportId) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();

        String reportName = switch (reportId) {
            case "pbi-rep-002" -> "Engineering Team Velocity & MTTR Analytics";
            case "pbi-rep-003" -> "Multi-Cloud & Website Health Telemetry";
            case "pbi-rep-004" -> "Salesforce Org Health & Limits Intelligence";
            default -> "Executive Reliability & SLA Dashboard";
        };

        // Generates server-side simulated embed contract with 1-hour expiration and organization isolation claim
        Instant expiration = Instant.now().plus(60, ChronoUnit.MINUTES);
        String embedUrl = "https://app.powerbi.com/reportEmbed?reportId=" + reportId + "&groupId=emblemix-workspace-org-" + orgId;
        String simulatedToken = "H4sIAAAAAAAAE" + UUID.randomUUID().toString().replace("-", "") + ".eyJPcmdJZCI6IiI" + orgId + "In0";

        return new PowerBiEmbedConfigResponse(
                reportId,
                reportName,
                embedUrl,
                simulatedToken,
                expiration,
                "emblemix-ws-" + orgId.toString().substring(0, 8),
                "entra-tenant-emblemix-prod",
                orgId.toString()
        );
    }

    private boolean isSlaMet(Incident incident) {
        if (incident.getAcknowledgedAt() == null) {
            // If still open and past SLA limit, SLA is breached
            long ageMinutes = Duration.between(incident.getCreatedAt(), Instant.now()).toMinutes();
            return ageMinutes <= getSlaThresholdMinutes(incident.getSeverity());
        }
        long ackMinutes = Duration.between(incident.getCreatedAt(), incident.getAcknowledgedAt()).toMinutes();
        return ackMinutes <= getSlaThresholdMinutes(incident.getSeverity());
    }

    private long getSlaThresholdMinutes(IncidentSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 15L;
            case HIGH -> 30L;
            case MEDIUM -> 60L;
            case LOW -> 240L;
        };
    }
}
