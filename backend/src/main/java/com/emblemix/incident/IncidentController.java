package com.emblemix.incident;

import com.emblemix.common.response.ApiResponse;
import com.emblemix.common.response.PagedResponse;
import com.emblemix.incident.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incidents", description = "Incident Management Lifecycle")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    @Operation(summary = "List all incidents with optional status or severity filters")
    public ResponseEntity<ApiResponse<PagedResponse<IncidentResponse>>> getIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentSeverity severity,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(incidentService.getIncidents(status, severity, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information for a specific incident including timeline and comments")
    public ResponseEntity<ApiResponse<IncidentDetailResponse>> getIncidentDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(incidentService.getIncidentDetails(id)));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Acknowledge an incident")
    public ResponseEntity<ApiResponse<IncidentResponse>> acknowledgeIncident(@PathVariable UUID id) {
        IncidentResponse response = incidentService.acknowledgeIncident(id);
        return ResponseEntity.ok(ApiResponse.success("Incident acknowledged successfully", response));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Assign an incident to an engineer")
    public ResponseEntity<ApiResponse<IncidentResponse>> assignIncident(
            @PathVariable UUID id,
            @Valid @RequestBody AssignIncidentRequest request) {
        IncidentResponse response = incidentService.assignIncident(id, request.userId());
        return ResponseEntity.ok(ApiResponse.success("Incident assigned successfully", response));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Resolve an incident with resolution notes")
    public ResponseEntity<ApiResponse<IncidentResponse>> resolveIncident(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveIncidentRequest request) {
        IncidentResponse response = incidentService.resolveIncident(id, request.resolutionNotes());
        return ResponseEntity.ok(ApiResponse.success("Incident resolved successfully", response));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a comment to an incident")
    public ResponseEntity<ApiResponse<IncidentCommentDto>> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody AddCommentRequest request) {
        IncidentCommentDto comment = incidentService.addComment(id, request.comment());
        return ResponseEntity.ok(ApiResponse.success("Comment added successfully", comment));
    }
}
