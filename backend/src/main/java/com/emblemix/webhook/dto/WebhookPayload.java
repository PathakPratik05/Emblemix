package com.emblemix.webhook.dto;

import java.time.Instant;
import java.util.Map;

public record WebhookPayload(
        String eventType,
        String severity,
        String message,
        String title,
        String status,
        Map<String, Object> details,
        Instant timestamp
) {}
