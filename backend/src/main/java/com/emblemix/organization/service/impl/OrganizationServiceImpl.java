package com.emblemix.organization.service.impl;

import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.common.security.SecurityUtils;
import com.emblemix.organization.Organization;
import com.emblemix.organization.OrganizationRepository;
import com.emblemix.organization.dto.OrganizationResponse;
import com.emblemix.organization.dto.UpdateOrganizationRequest;
import com.emblemix.organization.service.OrganizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationServiceImpl(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getCurrentOrganization() {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));
        return OrganizationResponse.from(org);
    }

    @Override
    @Transactional
    public OrganizationResponse updateCurrentOrganization(UpdateOrganizationRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));
        org.setName(request.name().trim());
        return OrganizationResponse.from(organizationRepository.save(org));
    }
}
