package com.emblemix.webhook.service.impl;

import com.emblemix.common.exception.BadRequestException;
import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.common.exception.UnauthorizedException;
import com.emblemix.common.util.AesEncryptionUtil;
import com.emblemix.event.Event;
import com.emblemix.event.EventSeverity;
import com.emblemix.event.EventSourceType;
import com.emblemix.event.EventType;
import com.emblemix.event.service.EventIngestionService;
import com.emblemix.organization.Organization;
import com.emblemix.organization.OrganizationRepository;
import com.emblemix.system.ConnectedSystem;
import com.emblemix.system.ConnectedSystemRepository;
import com.emblemix.system.SystemStatus;
import com.emblemix.system.SystemType;
import com.emblemix.webhook.*;
import com.emblemix.webhook.dto.*;
import com.emblemix.webhook.service.WebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookServiceImpl implements WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookServiceImpl.class);
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    private final WebhookConfigurationRepository webhookRepository;
    private final WebhookDeliveryLogRepository deliveryLogRepository;
    private final OrganizationRepository organizationRepository;
    private final ConnectedSystemRepository systemRepository;
    private final EventIngestionService eventIngestionService;
    private final AesEncryptionUtil aesEncryptionUtil;
    private final ObjectMapper objectMapper;

    public WebhookServiceImpl(WebhookConfigurationRepository webhookRepository,
                              WebhookDeliveryLogRepository deliveryLogRepository,
                              OrganizationRepository organizationRepository,
                              ConnectedSystemRepository systemRepository,
                              EventIngestionService eventIngestionService,
                              AesEncryptionUtil aesEncryptionUtil,
                              ObjectMapper objectMapper) {
        this.webhookRepository = webhookRepository;
        this.deliveryLogRepository = deliveryLogRepository;
        this.organizationRepository = organizationRepository;
        this.systemRepository = systemRepository;
        this.eventIngestionService = eventIngestionService;
        this.aesEncryptionUtil = aesEncryptionUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public WebhookResponse createWebhook(UUID orgId, CreateWebhookRequest request, String baseUrl) {
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

        ConnectedSystem system = null;
        if (request.systemId() != null) {
            system = systemRepository.findByIdAndOrganizationId(request.systemId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("ConnectedSystem", "id", request.systemId()));
        }

        String plainSecret = generateSecret();
        String encryptedSecret = aesEncryptionUtil.encrypt(plainSecret);

        WebhookConfiguration config = new WebhookConfiguration(
                null,
                organization,
                system,
                request.name().trim(),
                request.description() != null ? request.description().trim() : null,
                encryptedSecret,
                true
        );

        WebhookConfiguration saved = webhookRepository.save(config);
        log.info("Created custom webhook '{}' (id: {}) for org {}", saved.getName(), saved.getId(), orgId);

        return WebhookResponse.from(saved, plainSecret, baseUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookResponse> getWebhooks(UUID orgId, String baseUrl) {
        return webhookRepository.findByOrganizationId(orgId).stream()
                .map(w -> WebhookResponse.from(w, null, baseUrl))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WebhookResponse getWebhook(UUID orgId, UUID webhookId, String baseUrl) {
        WebhookConfiguration config = webhookRepository.findByIdAndOrganizationId(webhookId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookConfiguration", "id", webhookId));
        return WebhookResponse.from(config, null, baseUrl);
    }

    @Override
    @Transactional
    public WebhookResponse rotateSecret(UUID orgId, UUID webhookId, String baseUrl) {
        WebhookConfiguration config = webhookRepository.findByIdAndOrganizationId(webhookId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookConfiguration", "id", webhookId));

        String newSecret = generateSecret();
        config.setEncryptedSecret(aesEncryptionUtil.encrypt(newSecret));
        WebhookConfiguration saved = webhookRepository.save(config);

        log.info("Rotated secret for webhook '{}' (id: {})", saved.getName(), saved.getId());
        return WebhookResponse.from(saved, newSecret, baseUrl);
    }

    @Override
    @Transactional
    public void deleteWebhook(UUID orgId, UUID webhookId) {
        WebhookConfiguration config = webhookRepository.findByIdAndOrganizationId(webhookId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookConfiguration", "id", webhookId));
        webhookRepository.delete(config);
        log.info("Deleted webhook id: {} for org {}", webhookId, orgId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookDeliveryLogResponse> getDeliveryLogs(UUID orgId, UUID webhookId, int page, int size) {
        webhookRepository.findByIdAndOrganizationId(webhookId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookConfiguration", "id", webhookId));

        return deliveryLogRepository.findByWebhookIdOrderByReceivedAtDesc(webhookId, PageRequest.of(page, size)).stream()
                .map(WebhookDeliveryLogResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public WebhookIngestResult receiveWebhook(UUID webhookId, byte[] rawPayload, String signatureHeader) {
        WebhookConfiguration webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookConfiguration", "id", webhookId));

        String summary = rawPayload != null && rawPayload.length > 0
                ? new String(rawPayload, StandardCharsets.UTF_8).substring(0, Math.min(rawPayload.length, 500))
                : "empty payload";

        if (!webhook.isEnabled()) {
            recordLog(webhook, WebhookDeliveryStatus.SYSTEM_DISABLED, 403, "Webhook is disabled", summary);
            throw new BadRequestException("Webhook is disabled");
        }

        String plainSecret = aesEncryptionUtil.decrypt(webhook.getEncryptedSecret());
        if (!verifySignature(plainSecret, rawPayload, signatureHeader)) {
            recordLog(webhook, WebhookDeliveryStatus.INVALID_SIGNATURE, 401, "Invalid or missing HMAC signature", summary);
            throw new UnauthorizedException("Invalid HMAC signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);

            String message = extractField(root, "message", "title", "text", "description");
            if (message == null || message.isBlank()) {
                message = "Webhook alert notification from " + webhook.getName();
            }

            String statusStr = extractField(root, "status", "state", "action");
            boolean isRecovery = statusStr != null && (
                    statusStr.equalsIgnoreCase("RESOLVED") ||
                            statusStr.equalsIgnoreCase("RECOVERED") ||
                            statusStr.equalsIgnoreCase("OK") ||
                            statusStr.equalsIgnoreCase("UP")
            );

            EventType eventType = isRecovery ? EventType.WEBHOOK_RECOVERED : EventType.WEBHOOK_INCIDENT;

            String severityStr = extractField(root, "severity", "level", "priority");
            EventSeverity severity = mapSeverity(severityStr, isRecovery);

            ConnectedSystem system = webhook.getSystem();
            if (system == null) {
                system = getOrCreateDefaultWebhookSystem(webhook.getOrganization());
            }

            Event event = new Event(
                    null,
                    webhook.getOrganization(),
                    system,
                    EventSourceType.WEBHOOK,
                    eventType,
                    severity,
                    message,
                    root.toString(),
                    null,
                    Instant.now()
            );

            Event processed = eventIngestionService.processNormalizedEvent(event, null, null, null);

            recordLog(webhook, WebhookDeliveryStatus.SUCCESS, 200, null, summary);
            return new WebhookIngestResult("SUCCESS", "Webhook processed successfully", processed.getId());

        } catch (Exception e) {
            log.error("Failed to parse and process webhook payload for webhook id {}: {}", webhookId, e.getMessage());
            recordLog(webhook, WebhookDeliveryStatus.PROCESSING_ERROR, 500, e.getMessage(), summary);
            throw new BadRequestException("Failed to parse webhook JSON payload: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public WebhookIngestResult testWebhook(UUID orgId, UUID webhookId) {
        WebhookConfiguration webhook = webhookRepository.findByIdAndOrganizationId(webhookId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookConfiguration", "id", webhookId));

        String testJson = """
                {
                  "title": "Emblemix Simulator Test Ping",
                  "message": "Simulated webhook event for verification and alert routing.",
                  "severity": "HIGH",
                  "status": "FAIL",
                  "details": {
                    "source": "Emblemix Test Console",
                    "timestamp": "%s"
                  }
                }
                """.formatted(Instant.now().toString());

        byte[] payloadBytes = testJson.getBytes(StandardCharsets.UTF_8);
        String secret = aesEncryptionUtil.decrypt(webhook.getEncryptedSecret());
        String signature = computeHmacSha256(secret, payloadBytes);

        return receiveWebhook(webhookId, payloadBytes, "sha256=" + signature);
    }

    @Override
    public boolean verifySignature(String secret, byte[] payload, String providedSignature) {
        if (providedSignature == null || providedSignature.isBlank() || payload == null) {
            return false;
        }

        String providedSig = providedSignature.trim();
        if (providedSig.toLowerCase().startsWith("sha256=")) {
            providedSig = providedSig.substring(7).trim();
        }

        String expectedSig = computeHmacSha256(secret, payload);
        return MessageDigest.isEqual(
                expectedSig.toLowerCase().getBytes(StandardCharsets.UTF_8),
                providedSig.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public String computeHmacSha256(String secret, byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(payload != null ? payload : new byte[0]);
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating HMAC-SHA256 signature", e);
        }
    }

    private void recordLog(WebhookConfiguration webhook, WebhookDeliveryStatus status, int httpStatusCode, String errorMessage, String payloadSummary) {
        try {
            WebhookDeliveryLog deliveryLog = new WebhookDeliveryLog(
                    null,
                    webhook,
                    webhook.getOrganization(),
                    status,
                    httpStatusCode,
                    errorMessage,
                    payloadSummary,
                    Instant.now()
            );
            deliveryLogRepository.save(deliveryLog);
        } catch (Exception e) {
            log.error("Failed to persist webhook delivery log: {}", e.getMessage());
        }
    }

    private ConnectedSystem getOrCreateDefaultWebhookSystem(Organization org) {
        return systemRepository.findByOrganizationId(org.getId()).stream()
                .filter(s -> s.getSystemType() == SystemType.API)
                .findFirst()
                .orElseGet(() -> {
                    ConnectedSystem def = new ConnectedSystem(
                            null,
                            org,
                            "External Webhook Ingest",
                            SystemType.API,
                            "Auto-provisioned receiver system for external custom webhooks",
                            "PRODUCTION"
                    );
                    def.setStatus(SystemStatus.HEALTHY);
                    return systemRepository.save(def);
                });
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String extractField(JsonNode root, String... candidateFieldNames) {
        for (String fieldName : candidateFieldNames) {
            JsonNode node = root.get(fieldName);
            if (node != null && !node.isNull() && !node.asText().isBlank()) {
                return node.asText();
            }
        }
        return null;
    }

    private EventSeverity mapSeverity(String severityStr, boolean isRecovery) {
        if (isRecovery) {
            return EventSeverity.LOW;
        }
        if (severityStr == null) {
            return EventSeverity.MEDIUM;
        }
        return switch (severityStr.toUpperCase().trim()) {
            case "CRITICAL", "P1", "FATAL", "EMERGENCY" -> EventSeverity.CRITICAL;
            case "HIGH", "P2", "ERROR" -> EventSeverity.HIGH;
            case "LOW", "P4", "INFO", "DEBUG" -> EventSeverity.LOW;
            default -> EventSeverity.MEDIUM;
        };
    }
}
