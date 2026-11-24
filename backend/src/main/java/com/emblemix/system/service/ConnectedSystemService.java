package com.emblemix.system.service;

import com.emblemix.common.response.PagedResponse;
import com.emblemix.system.SystemStatus;
import com.emblemix.system.SystemType;
import com.emblemix.system.dto.ConnectedSystemRequest;
import com.emblemix.system.dto.ConnectedSystemResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

/**
 * Service interface for connected systems lifecycle, ownership, and health status updates.
 */
public interface ConnectedSystemService {

    /**
     * Registers a new connected system in the organization with optional team ownership and criticality.
     */
    ConnectedSystemResponse createSystem(ConnectedSystemRequest request);

    /**
     * Queries connected systems within the tenant, optionally filtered by system type.
     */
    PagedResponse<ConnectedSystemResponse> getSystems(SystemType systemType, Pageable pageable);

    /**
     * Retrieves system details by ID for the tenant.
     */
    ConnectedSystemResponse getSystemById(UUID id);

    /**
     * Updates connected system attributes, ownership assignments, and escalation policy.
     */
    ConnectedSystemResponse updateSystem(UUID id, ConnectedSystemRequest request);

    /**
     * Deletes a connected system from the organization.
     */
    void deleteSystem(UUID id);

    /**
     * Updates the operational status and last monitored timestamp of a connected system.
     */
    void updateSystemStatus(UUID id, SystemStatus status, Instant monitoredAt);
}
