package com.emblemix.organization.dto;

import com.emblemix.organization.Organization;
import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrganizationResponse from(Organization org) {
        return new OrganizationResponse(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getCreatedAt(),
                org.getUpdatedAt()
        );
    }
}
