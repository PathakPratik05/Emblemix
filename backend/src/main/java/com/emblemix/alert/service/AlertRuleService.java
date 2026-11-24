package com.emblemix.alert.service;

import com.emblemix.alert.dto.AlertRuleRequest;
import com.emblemix.alert.dto.AlertRuleResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for alert rules CRUD, evaluation thresholds, and target severity policies.
 */
public interface AlertRuleService {

    /**
     * Creates a new automated alert rule for a system or organization tenant.
     */
    AlertRuleResponse createRule(AlertRuleRequest request);

    /**
     * Lists alert rules configured for the current tenant.
     */
    List<AlertRuleResponse> getRules();

    /**
     * Retrieves an alert rule by ID.
     */
    AlertRuleResponse getRuleById(UUID id);

    /**
     * Updates threshold criteria or target severity of an existing alert rule.
     */
    AlertRuleResponse updateRule(UUID id, AlertRuleRequest request);

    /**
     * Deletes an alert rule.
     */
    void deleteRule(UUID id);
}
