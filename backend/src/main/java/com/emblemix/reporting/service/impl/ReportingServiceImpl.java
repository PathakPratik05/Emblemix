package com.emblemix.reporting.service.impl;

import com.emblemix.common.security.SecurityUtils;
import com.emblemix.incident.Incident;
import com.emblemix.incident.IncidentRepository;
import com.emblemix.incident.IncidentSeverity;
import com.emblemix.incident.IncidentStatus;
import com.emblemix.monitoring.website.MonitoringResult;
import com.emblemix.monitoring.website.MonitoringResultRepository;
import com.emblemix.reporting.dto.DashboardOverviewDto;
import com.emblemix.reporting.dto.IncidentTrendDto;
import com.emblemix.reporting.dto.UptimeReportDto;
import com.emblemix.reporting.service.ReportingService;
import com.emblemix.system.ConnectedSystem;
import com.emblemix.system.ConnectedSystemRepository;
import com.emblemix.system.SystemStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportingServiceImpl implements ReportingService {

    private final ConnectedSystemRepository systemRepository;
    private final IncidentRepository incidentRepository;
    private final MonitoringResultRepository resultRepository;

    public ReportingServiceImpl(ConnectedSystemRepository systemRepository,
                                IncidentRepository incidentRepository,
                                MonitoringResultRepository resultRepository) {
        this.systemRepository = systemRepository;
        this.incidentRepository = incidentRepository;
        this.resultRepository = resultRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewDto getDashboardOverview() {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();

        long totalSystems = systemRepository.countByOrganizationId(orgId);
        long healthySystems = systemRepository.countByOrganizationIdAndStatus(orgId, SystemStatus.HEALTHY);
        long degradedSystems = systemRepository.countByOrganizationIdAndStatus(orgId, SystemStatus.DEGRADED);
        long downSystems = systemRepository.countByOrganizationIdAndStatus(orgId, SystemStatus.DOWN);

        long openIncidents = incidentRepository.countByOrganizationIdAndStatus(orgId, IncidentStatus.OPEN) +
                incidentRepository.countByOrganizationIdAndStatus(orgId, IncidentStatus.ACKNOWLEDGED) +
                incidentRepository.countByOrganizationIdAndStatus(orgId, IncidentStatus.INVESTIGATING);

        long criticalIncidents = incidentRepository.countByOrganizationIdAndSeverity(orgId, IncidentSeverity.CRITICAL);

        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        long resolvedToday = incidentRepository.countByOrganizationIdAndResolvedAtAfter(orgId, startOfToday);

        // Average resolution time (last 30 days)
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Incident> resolvedIncidents = incidentRepository.findResolvedIncidentsSince(orgId, thirtyDaysAgo);
        double avgResolutionMinutes = 0.0;
        if (!resolvedIncidents.isEmpty()) {
            double totalMinutes = 0.0;
            for (Incident inc : resolvedIncidents) {
                if (inc.getResolvedAt() != null) {
                    totalMinutes += Duration.between(inc.getCreatedAt(), inc.getResolvedAt()).toMinutes();
                }
            }
            avgResolutionMinutes = Math.round((totalMinutes / resolvedIncidents.size()) * 10.0) / 10.0;
        }

        // Uptime and response time metrics (last 24h)
        Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        long totalChecks = resultRepository.countTotalChecksSince(orgId, oneDayAgo);
        long successfulChecks = resultRepository.countSuccessfulChecksSince(orgId, oneDayAgo);
        double uptimePercentage = totalChecks > 0
                ? Math.round(((double) successfulChecks / (double) totalChecks) * 1000.0) / 10.0
                : 100.0;

        Double avgResponseTime = resultRepository.getAverageResponseTimeSince(orgId, oneDayAgo);
        double responseTime = avgResponseTime != null ? Math.round(avgResponseTime * 10.0) / 10.0 : 0.0;

        Map<String, Long> systemHealthDistribution = Map.of(
                "HEALTHY", healthySystems,
                "DEGRADED", degradedSystems,
                "DOWN", downSystems,
                "UNKNOWN", Math.max(0, totalSystems - (healthySystems + degradedSystems + downSystems))
        );

        Map<String, Long> incidentSeverityDistribution = Map.of(
                "CRITICAL", incidentRepository.countByOrganizationIdAndSeverity(orgId, IncidentSeverity.CRITICAL),
                "HIGH", incidentRepository.countByOrganizationIdAndSeverity(orgId, IncidentSeverity.HIGH),
                "MEDIUM", incidentRepository.countByOrganizationIdAndSeverity(orgId, IncidentSeverity.MEDIUM),
                "LOW", incidentRepository.countByOrganizationIdAndSeverity(orgId, IncidentSeverity.LOW)
        );

        return new DashboardOverviewDto(
                totalSystems,
                healthySystems,
                degradedSystems,
                downSystems,
                openIncidents,
                criticalIncidents,
                resolvedToday,
                avgResolutionMinutes,
                uptimePercentage,
                responseTime,
                systemHealthDistribution,
                incidentSeverityDistribution
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentTrendDto> getIncidentTrends(int days) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        int safeDays = Math.max(1, Math.min(days, 90));
        Instant since = Instant.now().minus(safeDays, ChronoUnit.DAYS);

        List<Incident> incidents = incidentRepository.findResolvedIncidentsSince(orgId, since);
        Map<String, long[]> countsByDay = new LinkedHashMap<>();

        // Pre-populate days
        for (int i = safeDays - 1; i >= 0; i--) {
            String day = LocalDate.now(ZoneOffset.UTC).minusDays(i).toString();
            countsByDay.put(day, new long[]{0, 0}); // [created, resolved]
        }

        for (Incident inc : incidents) {
            String day = inc.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().toString();
            if (countsByDay.containsKey(day)) {
                countsByDay.get(day)[0]++;
            }
            if (inc.getResolvedAt() != null) {
                String resolvedDay = inc.getResolvedAt().atZone(ZoneOffset.UTC).toLocalDate().toString();
                if (countsByDay.containsKey(resolvedDay)) {
                    countsByDay.get(resolvedDay)[1]++;
                }
            }
        }

        return countsByDay.entrySet().stream()
                .map(e -> new IncidentTrendDto(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UptimeReportDto> getUptimeReports(int days) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Instant since = Instant.now().minus(Math.max(1, days), ChronoUnit.DAYS);

        List<ConnectedSystem> systems = systemRepository.findByOrganizationId(orgId);
        List<MonitoringResult> results = resultRepository.findResultsSince(orgId, since);

        Map<UUID, List<MonitoringResult>> resultsBySystem = results.stream()
                .collect(Collectors.groupingBy(r -> r.getSystem().getId()));

        List<UptimeReportDto> reports = new ArrayList<>();
        for (ConnectedSystem sys : systems) {
            List<MonitoringResult> sysResults = resultsBySystem.getOrDefault(sys.getId(), List.of());
            long total = sysResults.size();
            long success = sysResults.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
            long failed = total - success;
            double uptime = total > 0 ? Math.round(((double) success / total) * 1000.0) / 10.0 : 100.0;
            double avgLatency = sysResults.stream()
                    .filter(r -> r.getResponseTimeMs() != null)
                    .mapToLong(MonitoringResult::getResponseTimeMs)
                    .average()
                    .orElse(0.0);

            reports.add(new UptimeReportDto(
                    sys.getId(),
                    sys.getName(),
                    uptime,
                    Math.round(avgLatency * 10.0) / 10.0,
                    total,
                    success,
                    failed
            ));
        }

        return reports;
    }

    @Override
    @Transactional(readOnly = true)
    public String exportIncidentsCsv() {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        List<Incident> incidents = incidentRepository.findAll().stream()
                .filter(i -> i.getOrganization().getId().equals(orgId))
                .sorted(Comparator.comparing(Incident::getCreatedAt).reversed())
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("Incident ID,System Name,Title,Severity,Status,Created At,Resolved At,Resolution Notes\n");

        for (Incident inc : incidents) {
            sb.append(escapeCsv(inc.getId().toString())).append(",");
            sb.append(escapeCsv(inc.getSystem().getName())).append(",");
            sb.append(escapeCsv(inc.getTitle())).append(",");
            sb.append(escapeCsv(inc.getSeverity().name())).append(",");
            sb.append(escapeCsv(inc.getStatus().name())).append(",");
            sb.append(escapeCsv(inc.getCreatedAt().toString())).append(",");
            sb.append(escapeCsv(inc.getResolvedAt() != null ? inc.getResolvedAt().toString() : "")).append(",");
            sb.append(escapeCsv(inc.getResolutionNotes() != null ? inc.getResolutionNotes() : "")).append("\n");
        }

        return sb.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "\"\"";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}
