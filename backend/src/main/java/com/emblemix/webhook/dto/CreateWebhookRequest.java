package com.emblemix.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateWebhookRequest(
        @NotBlank(message = "Webhook name is required")
        String name,
        String description,
        UUID systemId
) {}
