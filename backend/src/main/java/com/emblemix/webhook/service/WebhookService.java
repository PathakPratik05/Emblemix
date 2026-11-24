package com.emblemix.webhook.service;

import com.emblemix.webhook.dto.CreateWebhookRequest;
import com.emblemix.webhook.dto.WebhookDeliveryLogResponse;
import com.emblemix.webhook.dto.WebhookIngestResult;
import com.emblemix.webhook.dto.WebhookResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for custom webhook endpoints, HMAC-SHA256 signature verification, and delivery logging.
 */
public interface WebhookService {

    /**
     * Registers a new custom webhook endpoint for inbound operational events.
     */
    WebhookResponse createWebhook(UUID orgId, CreateWebhookRequest request, String baseUrl);

    /**
     * Lists active custom webhooks configured for an organization.
     */
    List<WebhookResponse> getWebhooks(UUID orgId, String baseUrl);

    /**
     * Retrieves custom webhook configuration details by ID.
     */
    WebhookResponse getWebhook(UUID orgId, UUID webhookId, String baseUrl);

    /**
     * Rotates the HMAC-SHA256 signing secret for an inbound webhook.
     */
    WebhookResponse rotateSecret(UUID orgId, UUID webhookId, String baseUrl);

    /**
     * Deletes a custom webhook endpoint.
     */
    void deleteWebhook(UUID orgId, UUID webhookId);

    /**
     * Queries recent delivery logs and HMAC verification results for a webhook.
     */
    List<WebhookDeliveryLogResponse> getDeliveryLogs(UUID orgId, UUID webhookId, int page, int size);

    /**
     * Verifies HMAC signature, extracts normalized fields, and ingests inbound webhook payloads.
     */
    WebhookIngestResult receiveWebhook(UUID webhookId, byte[] rawPayload, String signatureHeader);

    /**
     * Sends a simulated test event through the webhook validation and normalization pipeline.
     */
    WebhookIngestResult testWebhook(UUID orgId, UUID webhookId);

    /**
     * Verifies HMAC-SHA256 signature against the provided raw payload and header.
     */
    boolean verifySignature(String secret, byte[] payloadBytes, String signatureHeader);

    /**
     * Computes hexadecimal HMAC-SHA256 signature string for a payload.
     */
    String computeHmacSha256(String secret, byte[] payload);
}
