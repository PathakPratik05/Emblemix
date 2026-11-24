package com.emblemix.webhook.dto;

import java.util.UUID;

public record WebhookIngestResult(
        String status,
        String message,
        UUID eventId
) {}
