package com.emblemix.organization.service;

import com.emblemix.organization.dto.OrganizationResponse;
import com.emblemix.organization.dto.UpdateOrganizationRequest;

/**
 * Service interface for organization tenant settings and profile management.
 */
public interface OrganizationService {

    /**
     * Retrieves the organization profile for the current tenant.
     */
    OrganizationResponse getCurrentOrganization();

    /**
     * Updates organization attributes such as name.
     */
    OrganizationResponse updateCurrentOrganization(UpdateOrganizationRequest request);
}
