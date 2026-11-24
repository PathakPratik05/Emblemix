package com.emblemix.system;

import com.emblemix.common.response.ApiResponse;
import com.emblemix.common.response.PagedResponse;
import com.emblemix.system.dto.ConnectedSystemRequest;
import com.emblemix.system.dto.ConnectedSystemResponse;
import com.emblemix.system.service.ConnectedSystemService;
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
@RequestMapping("/api/v1/systems")
@Tag(name = "Connected Systems", description = "Connected Systems Management")
public class ConnectedSystemController {

    private final ConnectedSystemService systemService;

    public ConnectedSystemController(ConnectedSystemService systemService) {
        this.systemService = systemService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Register a new monitored connected system")
    public ResponseEntity<ApiResponse<ConnectedSystemResponse>> createSystem(
            @Valid @RequestBody ConnectedSystemRequest request) {
        ConnectedSystemResponse response = systemService.createSystem(request);
        return new ResponseEntity<>(ApiResponse.success("Connected system created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all connected systems for current organization")
    public ResponseEntity<ApiResponse<PagedResponse<ConnectedSystemResponse>>> getSystems(
            @RequestParam(required = false) SystemType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(systemService.getSystems(type, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get details of a specific connected system")
    public ResponseEntity<ApiResponse<ConnectedSystemResponse>> getSystemById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(systemService.getSystemById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Update an existing connected system")
    public ResponseEntity<ApiResponse<ConnectedSystemResponse>> updateSystem(
            @PathVariable UUID id,
            @Valid @RequestBody ConnectedSystemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("System updated successfully",
                systemService.updateSystem(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Delete a connected system")
    public ResponseEntity<ApiResponse<Void>> deleteSystem(@PathVariable UUID id) {
        systemService.deleteSystem(id);
        return ResponseEntity.ok(ApiResponse.success("System deleted successfully", null));
    }
}
