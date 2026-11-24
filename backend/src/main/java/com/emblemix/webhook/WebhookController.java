package com.emblemix.webhook;

import com.emblemix.common.response.ApiResponse;
import com.emblemix.common.security.SecurityUtils;
import com.emblemix.webhook.dto.*;
import com.emblemix.webhook.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhook Integration", description = "Inbound Webhooks for third-party alerts and custom system monitoring")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping
    @Operation(summary = "List all configured webhooks for current organization")
    public ResponseEntity<ApiResponse<List<WebhookResponse>>> getWebhooks(HttpServletRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        String baseUrl = getBaseUrl(request);
        List<WebhookResponse> list = webhookService.getWebhooks(orgId, baseUrl);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping
    @Operation(summary = "Create a new custom webhook endpoint")
    public ResponseEntity<ApiResponse<WebhookResponse>> createWebhook(
            @Valid @RequestBody CreateWebhookRequest req,
            HttpServletRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        String baseUrl = getBaseUrl(request);
        WebhookResponse response = webhookService.createWebhook(orgId, req, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Webhook created successfully. Save secret securely.", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get webhook details by ID")
    public ResponseEntity<ApiResponse<WebhookResponse>> getWebhook(
            @PathVariable UUID id,
            HttpServletRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        String baseUrl = getBaseUrl(request);
        WebhookResponse response = webhookService.getWebhook(orgId, id, baseUrl);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/rotate-secret")
    @Operation(summary = "Rotate webhook signing secret")
    public ResponseEntity<ApiResponse<WebhookResponse>> rotateSecret(
            @PathVariable UUID id,
            HttpServletRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        String baseUrl = getBaseUrl(request);
        WebhookResponse response = webhookService.rotateSecret(orgId, id, baseUrl);
        return ResponseEntity.ok(ApiResponse.success("Secret rotated. Update external callers.", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a webhook")
    public ResponseEntity<ApiResponse<Void>> deleteWebhook(@PathVariable UUID id) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        webhookService.deleteWebhook(orgId, id);
        return ResponseEntity.ok(ApiResponse.success("Webhook deleted successfully", null));
    }

    @GetMapping("/{id}/deliveries")
    @Operation(summary = "Get delivery logs for a webhook")
    public ResponseEntity<ApiResponse<List<WebhookDeliveryLogResponse>>> getDeliveryLogs(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        List<WebhookDeliveryLogResponse> logs = webhookService.getDeliveryLogs(orgId, id, page, size);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "Simulate a test webhook delivery")
    public ResponseEntity<ApiResponse<WebhookIngestResult>> testWebhook(@PathVariable UUID id) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        WebhookIngestResult result = webhookService.testWebhook(orgId, id);
        return ResponseEntity.ok(ApiResponse.success("Test webhook executed successfully", result));
    }

    @PostMapping(value = "/receive/{id}", consumes = "application/json")
    @Operation(summary = "Public inbound webhook ingestion endpoint with HMAC-SHA256 signature verification")
    public ResponseEntity<ApiResponse<WebhookIngestResult>> receiveWebhook(
            @PathVariable UUID id,
            @RequestBody byte[] rawPayload,
            @RequestHeader(value = "X-Emblemix-Signature", required = false) String emblemixSig,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String hubSig) {

        String signature = emblemixSig != null && !emblemixSig.isBlank() ? emblemixSig : hubSig;
        WebhookIngestResult result = webhookService.receiveWebhook(id, rawPayload, signature);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null) {
            scheme = request.getScheme();
        }
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null) {
            host = request.getHeader("Host");
        }
        if (host == null) {
            return "http://localhost:8080";
        }
        return scheme + "://" + host;
    }
}
