package com.emblemix.notification;

import com.emblemix.organization.Organization;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_channels")
public class NotificationChannel {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private NotificationChannelType channelType = NotificationChannelType.EMAIL;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "notify_on_critical", nullable = false)
    private boolean notifyOnCritical = true;

    @Column(name = "notify_on_high", nullable = false)
    private boolean notifyOnHigh = true;

    @Column(name = "notify_on_resolve", nullable = false)
    private boolean notifyOnResolve = true;

    @Column(name = "notify_on_acknowledge", nullable = false)
    private boolean notifyOnAcknowledge = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public NotificationChannel() {}

    public NotificationChannel(UUID id, Organization organization, NotificationChannelType channelType,
                               String destination, boolean enabled, boolean notifyOnCritical,
                               boolean notifyOnHigh, boolean notifyOnResolve, boolean notifyOnAcknowledge) {
        this.id = id != null ? id : UUID.randomUUID();
        this.organization = organization;
        this.channelType = channelType;
        this.destination = destination;
        this.enabled = enabled;
        this.notifyOnCritical = notifyOnCritical;
        this.notifyOnHigh = notifyOnHigh;
        this.notifyOnResolve = notifyOnResolve;
        this.notifyOnAcknowledge = notifyOnAcknowledge;
        this.createdAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
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

    public NotificationChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(NotificationChannelType channelType) {
        this.channelType = channelType;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isNotifyOnCritical() {
        return notifyOnCritical;
    }

    public void setNotifyOnCritical(boolean notifyOnCritical) {
        this.notifyOnCritical = notifyOnCritical;
    }

    public boolean isNotifyOnHigh() {
        return notifyOnHigh;
    }

    public void setNotifyOnHigh(boolean notifyOnHigh) {
        this.notifyOnHigh = notifyOnHigh;
    }

    public boolean isNotifyOnResolve() {
        return notifyOnResolve;
    }

    public void setNotifyOnResolve(boolean notifyOnResolve) {
        this.notifyOnResolve = notifyOnResolve;
    }

    public boolean isNotifyOnAcknowledge() {
        return notifyOnAcknowledge;
    }

    public void setNotifyOnAcknowledge(boolean notifyOnAcknowledge) {
        this.notifyOnAcknowledge = notifyOnAcknowledge;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
