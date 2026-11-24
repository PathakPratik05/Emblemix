package com.emblemix.alert;

import com.emblemix.event.Event;
import com.emblemix.event.EventSeverity;
import com.emblemix.event.EventSourceType;
import com.emblemix.event.EventType;
import com.emblemix.organization.Organization;
import com.emblemix.system.ConnectedSystem;
import com.emblemix.system.SystemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertRuleEngineTest {

    @Mock
    private AlertRuleRepository ruleRepository;

    private AlertRuleEngine alertRuleEngine;

    private Organization org;
    private ConnectedSystem system;

    @BeforeEach
    void setUp() {
        alertRuleEngine = new AlertRuleEngine(ruleRepository);
        org = new Organization(UUID.randomUUID(), "Acme", "acme");
        system = new ConnectedSystem(UUID.randomUUID(), org, "Portal", SystemType.WEBSITE, null, "PRODUCTION");
    }

    @Test
    @DisplayName("Should trigger critical severity when failure count threshold is met")
    void testFailureCountThresholdTriggered() {
        Event event = new Event(
                UUID.randomUUID(), org, system, EventSourceType.WEBSITE,
                EventType.WEBSITE_DOWN, EventSeverity.MEDIUM, "Down", null, "fp", null
        );

        AlertRule rule = new AlertRule(
                UUID.randomUUID(), org, system, "3 Failures Rule",
                EventType.WEBSITE_DOWN, EventSeverity.CRITICAL, 3, null, null, true
        );

        when(ruleRepository.findApplicableRules(org.getId(), EventType.WEBSITE_DOWN, system.getId()))
                .thenReturn(List.of(rule));

        // When failures = 2 (< 3), does not trigger rule
        Optional<EventSeverity> result1 = alertRuleEngine.evaluateRules(event, 2, null, null);
        assertTrue(result1.isEmpty());

        // When failures = 3 (>= 3), triggers rule
        Optional<EventSeverity> result2 = alertRuleEngine.evaluateRules(event, 3, null, null);
        assertTrue(result2.isPresent());
        assertEquals(EventSeverity.CRITICAL, result2.get());
    }

    @Test
    @DisplayName("Should trigger rule when response time exceeds latency threshold")
    void testResponseTimeThresholdTriggered() {
        Event event = new Event(
                UUID.randomUUID(), org, system, EventSourceType.WEBSITE,
                EventType.HIGH_RESPONSE_TIME, EventSeverity.LOW, "Slow", null, "fp", null
        );

        AlertRule rule = new AlertRule(
                UUID.randomUUID(), org, system, "High Latency Rule",
                EventType.HIGH_RESPONSE_TIME, EventSeverity.HIGH, 1, 2000L, null, true
        );

        when(ruleRepository.findApplicableRules(org.getId(), EventType.HIGH_RESPONSE_TIME, system.getId()))
                .thenReturn(List.of(rule));

        // When latency is 1500ms (< 2000ms), rule does not trigger
        Optional<EventSeverity> result1 = alertRuleEngine.evaluateRules(event, 1, 1500L, null);
        assertTrue(result1.isEmpty());

        // When latency is 2500ms (>= 2000ms), rule triggers
        Optional<EventSeverity> result2 = alertRuleEngine.evaluateRules(event, 1, 2500L, null);
        assertTrue(result2.isPresent());
        assertEquals(EventSeverity.HIGH, result2.get());
    }
}
