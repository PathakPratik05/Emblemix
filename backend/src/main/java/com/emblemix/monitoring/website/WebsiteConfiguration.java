package com.emblemix.monitoring.website;

import com.emblemix.organization.Organization;
import com.emblemix.system.ConnectedSystem;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "website_configurations")
public class WebsiteConfiguration {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id", nullable = false, unique = true)
    private ConnectedSystem system;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "monitoring_interval_seconds", nullable = false)
    private int monitoringIntervalSeconds = 60;

    @Column(name = "request_timeout_ms", nullable = false)
    private int requestTimeoutMs = 5000;

    @Column(name = "expected_http_status_min", nullable = false)
    private int expectedHttpStatusMin = 200;

    @Column(name = "expected_http_status_max", nullable = false)
    private int expectedHttpStatusMax = 299;

    @Column(name = "expected_response_content", columnDefinition = "TEXT")
    private String expectedResponseContent;

    @Column(name = "check_ssl", nullable = false)
    private boolean checkSsl = true;

    @Column(name = "ssl_expiry_threshold_days", nullable = false)
    private int sslExpiryThresholdDays = 30;

    @Column(name = "consecutive_failure_threshold", nullable = false)
    private int consecutiveFailureThreshold = 2;

    @Column(name = "current_consecutive_failures", nullable = false)
    private int currentConsecutiveFailures = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WebsiteConfiguration() {}

    public WebsiteConfiguration(UUID id, ConnectedSystem system, Organization organization, String url,
                                int monitoringIntervalSeconds, int requestTimeoutMs,
                                int expectedHttpStatusMin, int expectedHttpStatusMax,
                                String expectedResponseContent, boolean checkSsl,
                                int sslExpiryThresholdDays, int consecutiveFailureThreshold) {
        this.id = id != null ? id : UUID.randomUUID();
        this.system = system;
        this.organization = organization;
        this.url = url;
        this.monitoringIntervalSeconds = monitoringIntervalSeconds;
        this.requestTimeoutMs = requestTimeoutMs;
        this.expectedHttpStatusMin = expectedHttpStatusMin;
        this.expectedHttpStatusMax = expectedHttpStatusMax;
        this.expectedResponseContent = expectedResponseContent;
        this.checkSsl = checkSsl;
        this.sslExpiryThresholdDays = sslExpiryThresholdDays;
        this.consecutiveFailureThreshold = consecutiveFailureThreshold;
        this.currentConsecutiveFailures = 0;
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

    public ConnectedSystem getSystem() {
        return system;
    }

    public void setSystem(ConnectedSystem system) {
        this.system = system;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getMonitoringIntervalSeconds() {
        return monitoringIntervalSeconds;
    }

    public void setMonitoringIntervalSeconds(int monitoringIntervalSeconds) {
        this.monitoringIntervalSeconds = monitoringIntervalSeconds;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public int getExpectedHttpStatusMin() {
        return expectedHttpStatusMin;
    }

    public void setExpectedHttpStatusMin(int expectedHttpStatusMin) {
        this.expectedHttpStatusMin = expectedHttpStatusMin;
    }

    public int getExpectedHttpStatusMax() {
        return expectedHttpStatusMax;
    }

    public void setExpectedHttpStatusMax(int expectedHttpStatusMax) {
        this.expectedHttpStatusMax = expectedHttpStatusMax;
    }

    public String getExpectedResponseContent() {
        return expectedResponseContent;
    }

    public void setExpectedResponseContent(String expectedResponseContent) {
        this.expectedResponseContent = expectedResponseContent;
    }

    public boolean isCheckSsl() {
        return checkSsl;
    }

    public void setCheckSsl(boolean checkSsl) {
        this.checkSsl = checkSsl;
    }

    public int getSslExpiryThresholdDays() {
        return sslExpiryThresholdDays;
    }

    public void setSslExpiryThresholdDays(int sslExpiryThresholdDays) {
        this.sslExpiryThresholdDays = sslExpiryThresholdDays;
    }

    public int getConsecutiveFailureThreshold() {
        return consecutiveFailureThreshold;
    }

    public void setConsecutiveFailureThreshold(int consecutiveFailureThreshold) {
        this.consecutiveFailureThreshold = consecutiveFailureThreshold;
    }

    public int getCurrentConsecutiveFailures() {
        return currentConsecutiveFailures;
    }

    public void setCurrentConsecutiveFailures(int currentConsecutiveFailures) {
        this.currentConsecutiveFailures = currentConsecutiveFailures;
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
