package com.emblemix.system;

import com.emblemix.organization.Organization;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connected_systems")
public class ConnectedSystem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_type", nullable = false)
    private SystemType systemType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String environment = "PRODUCTION";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SystemStatus status = SystemStatus.UNKNOWN;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_monitored_at")
    private Instant lastMonitoredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ConnectedSystem() {}

    public ConnectedSystem(UUID id, Organization organization, String name, SystemType systemType,
                           String description, String environment) {
        this.id = id != null ? id : UUID.randomUUID();
        this.organization = organization;
        this.name = name;
        this.systemType = systemType;
        this.description = description;
        this.environment = environment != null ? environment : "PRODUCTION";
        this.status = SystemStatus.UNKNOWN;
        this.enabled = true;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SystemType getSystemType() {
        return systemType;
    }

    public void setSystemType(SystemType systemType) {
        this.systemType = systemType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public SystemStatus getStatus() {
        return status;
    }

    public void setStatus(SystemStatus status) {
        this.status = status;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getLastMonitoredAt() {
        return lastMonitoredAt;
    }

    public void setLastMonitoredAt(Instant lastMonitoredAt) {
        this.lastMonitoredAt = lastMonitoredAt;
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
