package com.emblemix.reporting;

import com.emblemix.common.response.ApiResponse;
import com.emblemix.reporting.dto.DashboardOverviewDto;
import com.emblemix.reporting.dto.IncidentTrendDto;
import com.emblemix.reporting.dto.UptimeReportDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Dashboard Analytics, Uptime Metrics, and Incident Exports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get centralized dashboard metrics, status counters, and severity distribution")
    public ResponseEntity<ApiResponse<DashboardOverviewDto>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(reportingService.getDashboardOverview()));
    }

    @GetMapping("/incidents")
    @Operation(summary = "Get historical incident creation and resolution trends")
    public ResponseEntity<ApiResponse<List<IncidentTrendDto>>> getIncidentTrends(
            @RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(ApiResponse.success(reportingService.getIncidentTrends(days)));
    }

    @GetMapping("/uptime")
    @Operation(summary = "Get system uptime and average latency reports")
    public ResponseEntity<ApiResponse<List<UptimeReportDto>>> getUptimeReports(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.success(reportingService.getUptimeReports(days)));
    }

    @GetMapping("/export/incidents.csv")
    @Operation(summary = "Export incident records to CSV format")
    public ResponseEntity<String> exportIncidentsCsv() {
        String csv = reportingService.exportIncidentsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"emblemix_incidents.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
