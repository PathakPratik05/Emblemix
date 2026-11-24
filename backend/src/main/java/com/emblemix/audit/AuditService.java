package com.emblemix.audit;

import com.emblemix.common.response.PagedResponse;
import com.emblemix.common.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getAuditLogs(Pageable pageable) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Page<AuditLog> page = auditLogRepository.findByOrganizationId(orgId, pageable);
        return new PagedResponse<>(
                page.map(AuditLogResponse::from).getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional
    public void log(com.emblemix.organization.Organization org, com.emblemix.user.User user, String action, String entityType, String entityId, String detailsJson) {
        AuditLog auditLog = new AuditLog(null, org, user, action, entityType, entityId, detailsJson, null);
        auditLogRepository.save(auditLog);
    }
}
