package com.emblemix.monitoring.salesforce;

import com.emblemix.organization.Organization;
import com.emblemix.system.ConnectedSystem;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "salesforce_configurations")
public class SalesforceConfiguration {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id", nullable = false, unique = true)
    private ConnectedSystem system;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "is_sandbox", nullable = false)
    private boolean isSandbox = false;

    @Column(name = "api_usage_warning_threshold_pct", nullable = false)
    private double apiUsageWarningThresholdPct = 80.0;

    @Column(name = "api_usage_critical_threshold_pct", nullable = false)
    private double apiUsageCriticalThresholdPct = 95.0;

    @Column(name = "monitor_apex_jobs", nullable = false)
    private boolean monitorApexJobs = true;

    @Column(name = "monitor_login_activity", nullable = false)
    private boolean monitorLoginActivity = true;

    @Column(name = "last_apex_job_checked_at")
    private Instant lastApexJobCheckedAt;

    @Column(name = "last_login_checked_at")
    private Instant lastLoginCheckedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SalesforceConfiguration() {}

    public SalesforceConfiguration(UUID id, ConnectedSystem system, Organization organization,
                                   boolean isSandbox, double apiUsageWarningThresholdPct,
                                   double apiUsageCriticalThresholdPct, boolean monitorApexJobs,
                                   boolean monitorLoginActivity) {
        this.id = id != null ? id : UUID.randomUUID();
        this.system = system;
        this.organization = organization;
        this.isSandbox = isSandbox;
        this.apiUsageWarningThresholdPct = apiUsageWarningThresholdPct;
        this.apiUsageCriticalThresholdPct = apiUsageCriticalThresholdPct;
        this.monitorApexJobs = monitorApexJobs;
        this.monitorLoginActivity = monitorLoginActivity;
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

    public boolean isSandbox() {
        return isSandbox;
    }

    public void setSandbox(boolean sandbox) {
        isSandbox = sandbox;
    }

    public double getApiUsageWarningThresholdPct() {
        return apiUsageWarningThresholdPct;
    }

    public void setApiUsageWarningThresholdPct(double apiUsageWarningThresholdPct) {
        this.apiUsageWarningThresholdPct = apiUsageWarningThresholdPct;
    }

    public double getApiUsageCriticalThresholdPct() {
        return apiUsageCriticalThresholdPct;
    }

    public void setApiUsageCriticalThresholdPct(double apiUsageCriticalThresholdPct) {
        this.apiUsageCriticalThresholdPct = apiUsageCriticalThresholdPct;
    }

    public boolean isMonitorApexJobs() {
        return monitorApexJobs;
    }

    public void setMonitorApexJobs(boolean monitorApexJobs) {
        this.monitorApexJobs = monitorApexJobs;
    }

    public boolean isMonitorLoginActivity() {
        return monitorLoginActivity;
    }

    public void setMonitorLoginActivity(boolean monitorLoginActivity) {
        this.monitorLoginActivity = monitorLoginActivity;
    }

    public Instant getLastApexJobCheckedAt() {
        return lastApexJobCheckedAt;
    }

    public void setLastApexJobCheckedAt(Instant lastApexJobCheckedAt) {
        this.lastApexJobCheckedAt = lastApexJobCheckedAt;
    }

    public Instant getLastLoginCheckedAt() {
        return lastLoginCheckedAt;
    }

    public void setLastLoginCheckedAt(Instant lastLoginCheckedAt) {
        this.lastLoginCheckedAt = lastLoginCheckedAt;
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
