package com.emblemix.monitoring.website;

import com.emblemix.common.response.ApiResponse;
import com.emblemix.common.response.PagedResponse;
import com.emblemix.monitoring.website.dto.*;
import com.emblemix.monitoring.website.service.WebsiteMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/websites")
@Tag(name = "Website Monitoring", description = "Website Availability and Performance Monitoring")
public class WebsiteController {

    private final WebsiteMonitoringService websiteService;

    public WebsiteController(WebsiteMonitoringService websiteService) {
        this.websiteService = websiteService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Register a new website for automated monitoring")
    public ResponseEntity<ApiResponse<WebsiteConfigResponse>> createWebsite(
            @Valid @RequestBody WebsiteConfigRequest request) {
        WebsiteConfigResponse response = websiteService.createWebsite(request);
        return new ResponseEntity<>(ApiResponse.success("Website registered for monitoring", response), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List monitored websites with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<WebsiteConfigResponse>>> getWebsites(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(websiteService.getWebsites(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get website configuration by ID")
    public ResponseEntity<ApiResponse<WebsiteConfigResponse>> getWebsiteById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(websiteService.getWebsiteById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Update website monitoring configuration")
    public ResponseEntity<ApiResponse<WebsiteConfigResponse>> updateWebsite(
            @PathVariable UUID id,
            @Valid @RequestBody WebsiteConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Website configuration updated",
                websiteService.updateWebsite(id, request)));
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "Execute an on-demand live test check against the target website")
    public ResponseEntity<ApiResponse<WebsiteTestResultDto>> testWebsite(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(websiteService.testWebsite(id)));
    }

    @GetMapping("/{id}/monitoring-history")
    @Operation(summary = "Get historical check results and latency metrics for a website")
    public ResponseEntity<ApiResponse<PagedResponse<MonitoringResultDto>>> getMonitoringHistory(
            @PathVariable UUID id,
            @PageableDefault(size = 30, sort = "checkedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(websiteService.getMonitoringHistory(id, pageable)));
    }
}
