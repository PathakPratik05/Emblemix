package com.emblemix.event;

import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.common.response.ApiResponse;
import com.emblemix.common.response.PagedResponse;
import com.emblemix.common.security.SecurityUtils;
import com.emblemix.event.dto.EventIngestRequest;
import com.emblemix.event.dto.EventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events", description = "Common Event Ingestion and Audit Feed")
public class EventController {

    private final EventIngestionService ingestionService;
    private final EventRepository eventRepository;

    public EventController(EventIngestionService ingestionService, EventRepository eventRepository) {
        this.ingestionService = ingestionService;
        this.eventRepository = eventRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Ingest a new monitoring or integration event")
    public ResponseEntity<ApiResponse<EventResponse>> ingestEvent(@Valid @RequestBody EventIngestRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        EventResponse response = ingestionService.ingestEvent(orgId, request);
        return new ResponseEntity<>(ApiResponse.success("Event ingested successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List normalized events with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<EventResponse>>> getEvents(
            @RequestParam(required = false) UUID systemId,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Page<Event> page = (systemId != null)
                ? eventRepository.findByOrganizationIdAndSystemId(orgId, systemId, pageable)
                : eventRepository.findByOrganizationId(orgId, pageable);

        PagedResponse<EventResponse> response = new PagedResponse<>(
                page.map(EventResponse::from).getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an event by ID")
    public ResponseEntity<ApiResponse<EventResponse>> getEventById(@PathVariable UUID id) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Event event = eventRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));
        return ResponseEntity.ok(ApiResponse.success(EventResponse.from(event)));
    }
}
