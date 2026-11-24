package com.emblemix.monitoring.website;

import com.emblemix.organization.Organization;
import com.emblemix.system.ConnectedSystem;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monitoring_results")
public class MonitoringResult {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id", nullable = false)
    private ConnectedSystem system;

    @Column(nullable = false)
    private String status; // SUCCESS, FAILURE, WARNING

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "ssl_valid")
    private Boolean sslValid;

    @Column(name = "ssl_days_remaining")
    private Integer sslDaysRemaining;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "raw_metrics_json", columnDefinition = "TEXT")
    private String rawMetricsJson;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    public MonitoringResult() {}

    public MonitoringResult(UUID id, Organization organization, ConnectedSystem system,
                            String status, Integer httpStatusCode, Long responseTimeMs,
                            Boolean sslValid, Integer sslDaysRemaining, String errorMessage,
                            String rawMetricsJson) {
        this.id = id != null ? id : UUID.randomUUID();
        this.organization = organization;
        this.system = system;
        this.status = status;
        this.httpStatusCode = httpStatusCode;
        this.responseTimeMs = responseTimeMs;
        this.sslValid = sslValid;
        this.sslDaysRemaining = sslDaysRemaining;
        this.errorMessage = errorMessage;
        this.rawMetricsJson = rawMetricsJson;
        this.checkedAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.checkedAt == null) {
            this.checkedAt = Instant.now();
        }
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public Boolean getSslValid() {
        return sslValid;
    }

    public void setSslValid(Boolean sslValid) {
        this.sslValid = sslValid;
    }

    public Integer getSslDaysRemaining() {
        return sslDaysRemaining;
    }

    public void setSslDaysRemaining(Integer sslDaysRemaining) {
        this.sslDaysRemaining = sslDaysRemaining;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRawMetricsJson() {
        return rawMetricsJson;
    }

    public void setRawMetricsJson(String rawMetricsJson) {
        this.rawMetricsJson = rawMetricsJson;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }
}
