package com.emblemix.webhook.dto;

import com.emblemix.webhook.WebhookConfiguration;

import java.time.Instant;
import java.util.UUID;

public record WebhookResponse(
        UUID id,
        UUID organizationId,
        UUID systemId,
        String systemName,
        String name,
        String description,
        String webhookUrl,
        String secret,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public static WebhookResponse from(WebhookConfiguration config, String plainSecret, String baseUrl) {
        String url = baseUrl + "/api/v1/webhooks/receive/" + config.getId();
        return new WebhookResponse(
                config.getId(),
                config.getOrganization().getId(),
                config.getSystem() != null ? config.getSystem().getId() : null,
                config.getSystem() != null ? config.getSystem().getName() : null,
                config.getName(),
                config.getDescription(),
                url,
                plainSecret,
                config.isEnabled(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
