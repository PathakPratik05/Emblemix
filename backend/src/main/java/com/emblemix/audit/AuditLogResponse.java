package com.emblemix.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID organizationId,
        UUID userId,
        String userName,
        String action,
        String entityType,
        String entityId,
        String detailsJson,
        String ipAddress,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        String userName = "System";
        if (log.getUser() != null) {
            userName = log.getUser().getFirstName() + " " + log.getUser().getLastName();
        }
        return new AuditLogResponse(
                log.getId(),
                log.getOrganization().getId(),
                log.getUser() != null ? log.getUser().getId() : null,
                userName,
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetailsJson(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}
