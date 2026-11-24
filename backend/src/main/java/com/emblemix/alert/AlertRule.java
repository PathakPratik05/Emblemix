package com.emblemix.alert;

import com.emblemix.event.EventSeverity;
import com.emblemix.event.EventType;
import com.emblemix.organization.Organization;
import com.emblemix.system.ConnectedSystem;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert_rules")
public class AlertRule {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id")
    private ConnectedSystem system; // null applies to all systems in org

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_severity", nullable = false)
    private EventSeverity targetSeverity;

    @Column(name = "failure_count_threshold", nullable = false)
    private int failureCountThreshold = 1;

    @Column(name = "response_time_threshold_ms")
    private Long responseTimeThresholdMs;

    @Column(name = "api_usage_pct_threshold")
    private Double apiUsagePctThreshold;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AlertRule() {}

    public AlertRule(UUID id, Organization organization, ConnectedSystem system, String name,
                     EventType eventType, EventSeverity targetSeverity, int failureCountThreshold,
                     Long responseTimeThresholdMs, Double apiUsagePctThreshold, boolean enabled) {
        this.id = id != null ? id : UUID.randomUUID();
        this.organization = organization;
        this.system = system;
        this.name = name;
        this.eventType = eventType;
        this.targetSeverity = targetSeverity;
        this.failureCountThreshold = failureCountThreshold;
        this.responseTimeThresholdMs = responseTimeThresholdMs;
        this.apiUsagePctThreshold = apiUsagePctThreshold;
        this.enabled = enabled;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public ConnectedSystem getSystem() {
        return system;
    }

    public void setSystem(ConnectedSystem system) {
        this.system = system;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public EventSeverity getTargetSeverity() {
        return targetSeverity;
    }

    public void setTargetSeverity(EventSeverity targetSeverity) {
        this.targetSeverity = targetSeverity;
    }

    public int getFailureCountThreshold() {
        return failureCountThreshold;
    }

    public void setFailureCountThreshold(int failureCountThreshold) {
        this.failureCountThreshold = failureCountThreshold;
    }

    public Long getResponseTimeThresholdMs() {
        return responseTimeThresholdMs;
    }

    public void setResponseTimeThresholdMs(Long responseTimeThresholdMs) {
        this.responseTimeThresholdMs = responseTimeThresholdMs;
    }

    public Double getApiUsagePctThreshold() {
        return apiUsagePctThreshold;
    }

    public void setApiUsagePctThreshold(Double apiUsagePctThreshold) {
        this.apiUsagePctThreshold = apiUsagePctThreshold;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
