package com.emblemix.alert;

import com.emblemix.alert.dto.AlertRuleRequest;
import com.emblemix.alert.dto.AlertRuleResponse;
import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.common.security.SecurityUtils;
import com.emblemix.organization.Organization;
import com.emblemix.organization.OrganizationRepository;
import com.emblemix.system.ConnectedSystem;
import com.emblemix.system.ConnectedSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AlertRuleService {

    private final AlertRuleRepository ruleRepository;
    private final OrganizationRepository organizationRepository;
    private final ConnectedSystemRepository systemRepository;

    public AlertRuleService(AlertRuleRepository ruleRepository,
                            OrganizationRepository organizationRepository,
                            ConnectedSystemRepository systemRepository) {
        this.ruleRepository = ruleRepository;
        this.organizationRepository = organizationRepository;
        this.systemRepository = systemRepository;
    }

    @Transactional
    public AlertRuleResponse createRule(AlertRuleRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

        ConnectedSystem system = null;
        if (request.systemId() != null) {
            system = systemRepository.findByIdAndOrganizationId(request.systemId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("ConnectedSystem", "id", request.systemId()));
        }

        AlertRule rule = new AlertRule(
                UUID.randomUUID(),
                org,
                system,
                request.name().trim(),
                request.eventType(),
                request.targetSeverity(),
                request.failureCountThreshold() != null ? request.failureCountThreshold() : 1,
                request.responseTimeThresholdMs(),
                request.apiUsagePctThreshold(),
                request.enabled() != null ? request.enabled() : true
        );

        return AlertRuleResponse.from(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<AlertRuleResponse> getRules() {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        return ruleRepository.findByOrganizationId(orgId).stream()
                .map(AlertRuleResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlertRuleResponse getRuleById(UUID id) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        AlertRule rule = ruleRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("AlertRule", "id", id));
        return AlertRuleResponse.from(rule);
    }

    @Transactional
    public AlertRuleResponse updateRule(UUID id, AlertRuleRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        AlertRule rule = ruleRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("AlertRule", "id", id));

        ConnectedSystem system = null;
        if (request.systemId() != null) {
            system = systemRepository.findByIdAndOrganizationId(request.systemId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("ConnectedSystem", "id", request.systemId()));
        }

        rule.setSystem(system);
        rule.setName(request.name().trim());
        rule.setEventType(request.eventType());
        rule.setTargetSeverity(request.targetSeverity());
        if (request.failureCountThreshold() != null) {
            rule.setFailureCountThreshold(request.failureCountThreshold());
        }
        rule.setResponseTimeThresholdMs(request.responseTimeThresholdMs());
        rule.setApiUsagePctThreshold(request.apiUsagePctThreshold());
        if (request.enabled() != null) {
            rule.setEnabled(request.enabled());
        }

        return AlertRuleResponse.from(ruleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(UUID id) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        AlertRule rule = ruleRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("AlertRule", "id", id));
        ruleRepository.delete(rule);
    }
}
