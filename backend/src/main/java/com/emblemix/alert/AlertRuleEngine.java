package com.emblemix.alert;

import com.emblemix.event.Event;
import com.emblemix.event.EventSeverity;
import com.emblemix.event.EventType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AlertRuleEngine {

    private final AlertRuleRepository ruleRepository;

    public AlertRuleEngine(AlertRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public Optional<EventSeverity> evaluateRules(Event event, Integer consecutiveFailures, Long responseTimeMs, Double apiUsagePct) {
        List<AlertRule> rules = ruleRepository.findApplicableRules(
                event.getOrganization().getId(),
                event.getEventType(),
                event.getSystem().getId()
        );

        if (rules.isEmpty()) {
            return Optional.empty();
        }

        for (AlertRule rule : rules) {
            boolean triggered = true;

            // Check failure count threshold
            if (rule.getFailureCountThreshold() > 1) {
                if (consecutiveFailures == null || consecutiveFailures < rule.getFailureCountThreshold()) {
                    triggered = false;
                }
            }

            // Check response time threshold
            if (triggered && rule.getResponseTimeThresholdMs() != null) {
                if (responseTimeMs == null || responseTimeMs < rule.getResponseTimeThresholdMs()) {
                    triggered = false;
                }
            }

            // Check API usage percentage threshold
            if (triggered && rule.getApiUsagePctThreshold() != null) {
                if (apiUsagePct == null || apiUsagePct < rule.getApiUsagePctThreshold()) {
                    triggered = false;
                }
            }

            if (triggered) {
                return Optional.of(rule.getTargetSeverity());
            }
        }

        return Optional.empty();
    }
}
