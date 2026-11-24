package com.emblemix.webhook.dto;

import com.emblemix.webhook.WebhookDeliveryLog;
import com.emblemix.webhook.WebhookDeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record WebhookDeliveryLogResponse(
        UUID id,
        UUID webhookId,
        WebhookDeliveryStatus status,
        int httpStatusCode,
        String errorMessage,
        String payloadSummary,
        Instant receivedAt
) {
    public static WebhookDeliveryLogResponse from(WebhookDeliveryLog log) {
        return new WebhookDeliveryLogResponse(
                log.getId(),
                log.getWebhook().getId(),
                log.getStatus(),
                log.getHttpStatusCode(),
                log.getErrorMessage(),
                log.getPayloadSummary(),
                log.getReceivedAt()
        );
    }
}
