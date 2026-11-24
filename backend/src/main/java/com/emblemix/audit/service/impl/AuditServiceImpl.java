package com.emblemix.audit.service.impl;

import com.emblemix.audit.AuditLog;
import com.emblemix.audit.AuditLogRepository;
import com.emblemix.audit.AuditLogResponse;
import com.emblemix.audit.service.AuditService;
import com.emblemix.common.response.PagedResponse;
import com.emblemix.common.security.SecurityUtils;
import com.emblemix.organization.Organization;
import com.emblemix.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
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

    @Override
    @Transactional
    public void log(Organization org, User user, String action, String entityType, String entityId, String detailsJson) {
        AuditLog auditLog = new AuditLog(null, org, user, action, entityType, entityId, detailsJson, null);
        auditLogRepository.save(auditLog);
    }
}
