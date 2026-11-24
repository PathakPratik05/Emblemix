package com.emblemix.audit.service;

import com.emblemix.audit.AuditLogResponse;
import com.emblemix.common.response.PagedResponse;
import com.emblemix.organization.Organization;
import com.emblemix.user.User;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for organization-level immutable audit logging and querying.
 */
public interface AuditService {

    /**
     * Queries paginated tenant audit log entries.
     */
    PagedResponse<AuditLogResponse> getAuditLogs(Pageable pageable);

    /**
     * Appends an immutable audit event entry recording administrative actions and entity modifications.
     */
    void log(Organization org, User user, String action, String entityType, String entityId, String detailsJson);
}
