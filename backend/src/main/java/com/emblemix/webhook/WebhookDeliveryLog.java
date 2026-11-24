package com.emblemix.webhook;

import com.emblemix.organization.Organization;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_delivery_logs")
public class WebhookDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_id", nullable = false)
    private WebhookConfiguration webhook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private WebhookDeliveryStatus status;

    @Column(name = "http_status_code", nullable = false)
    private int httpStatusCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "payload_summary", length = 1000)
    private String payloadSummary;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    public WebhookDeliveryLog() {}

    public WebhookDeliveryLog(UUID id, WebhookConfiguration webhook, Organization organization,
                              WebhookDeliveryStatus status, int httpStatusCode, String errorMessage,
                              String payloadSummary, Instant receivedAt) {
        this.id = id;
        this.webhook = webhook;
        this.organization = organization;
        this.status = status;
        this.httpStatusCode = httpStatusCode;
        this.errorMessage = errorMessage;
        this.payloadSummary = payloadSummary;
        this.receivedAt = receivedAt != null ? receivedAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public WebhookConfiguration getWebhook() {
        return webhook;
    }

    public void setWebhook(WebhookConfiguration webhook) {
        this.webhook = webhook;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public WebhookDeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(WebhookDeliveryStatus status) {
        this.status = status;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getPayloadSummary() {
        return payloadSummary;
    }

    public void setPayloadSummary(String payloadSummary) {
        this.payloadSummary = payloadSummary;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }
}
