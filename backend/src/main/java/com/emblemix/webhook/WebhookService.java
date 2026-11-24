package com.emblemix.webhook;

import com.emblemix.common.exception.BadRequestException;
import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.common.exception.UnauthorizedException;
import com.emblemix.common.util.AesEncryptionUtil;
import com.emblemix.event.*;
import com.emblemix.organization.Organization;
import com.emblemix.organization.OrganizationRepository;
import com.emblemix.system.ConnectedSystem;
import com.emblemix.system.ConnectedSystemRepository;
import com.emblemix.system.SystemStatus;
import com.emblemix.system.SystemType;
import com.emblemix.webhook.dto.*;
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
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    private final WebhookConfigurationRepository webhookRepository;
    private final WebhookDeliveryLogRepository deliveryLogRepository;
    private final OrganizationRepository organizationRepository;
    private final ConnectedSystemRepository systemRepository;
    private final EventIngestionService eventIngestionService;
    private final AesEncryptionUtil aesEncryptionUtil;
    private final ObjectMapper objectMapper;

    public WebhookService(WebhookConfigurationRepository webhookRepository,
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

    @Transactional(readOnly = true)
    public List<WebhookResponse> getWebhooks(UUID orgId, String baseUrl) {
        return webhookRepository.findByOrganizationId(orgId).stream()
                .map(w -> WebhookResponse.from(w, null, baseUrl))
                .toList();
    }

    @Transactional(readOnly = true)
    public WebhookResponse getWebhook(UUID orgId, UUID webhookId, String baseUrl) {
        WebhookConfiguration config = webhookRepository.findByIdAndOrganizationId(webhookId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookConfiguration", "id", webhookId));
        return WebhookResponse.from(config, null, baseUrl);
    }

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

    @Transactional
    public void deleteWebhook(UUID orgId, UUID webhookId) {
        WebhookConfiguration config = webhookRepository.findByIdAndOrganizationId(webhookId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookConfiguration", "id", webhookId));
        webhookRepository.delete(config);
        log.info("Deleted webhook id: {} for org {}", webhookId, orgId);
    }

    @Transactional(readOnly = true)
    public List<WebhookDeliveryLogResponse> getDeliveryLogs(UUID orgId, UUID webhookId, int page, int size) {
        webhookRepository.findByIdAndOrganizationId(webhookId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookConfiguration", "id", webhookId));

        return deliveryLogRepository.findByWebhookIdOrderByReceivedAtDesc(webhookId, PageRequest.of(page, size)).stream()
                .map(WebhookDeliveryLogResponse::from)
                .toList();
    }

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
            log.error("Failed to parse and process webhook payload for webhook {}", webhookId, e);
            recordLog(webhook, WebhookDeliveryStatus.INVALID_PAYLOAD, 400, "Invalid payload format: " + e.getMessage(), summary);
            throw new BadRequestException("Failed to process webhook payload: " + e.getMessage());
        }
    }

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

    public boolean verifySignature(String secret, byte[] payloadBytes, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank() || payloadBytes == null) {
            return false;
        }

        String providedSig = signatureHeader.trim();
        if (providedSig.toLowerCase().startsWith("sha256=")) {
            providedSig = providedSig.substring(7).trim();
        }

        String expectedSig = computeHmacSha256(secret, payloadBytes);
        return MessageDigest.isEqual(
                expectedSig.toLowerCase().getBytes(StandardCharsets.UTF_8),
                providedSig.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String computeHmacSha256(String secret, byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(payload);
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating HMAC-SHA256 signature", e);
        }
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private void recordLog(WebhookConfiguration webhook, WebhookDeliveryStatus status,
                           int statusCode, String errorMsg, String payloadSummary) {
        try {
            WebhookDeliveryLog deliveryLog = new WebhookDeliveryLog(
                    null,
                    webhook,
                    webhook.getOrganization(),
                    status,
                    statusCode,
                    errorMsg,
                    payloadSummary,
                    Instant.now()
            );
            deliveryLogRepository.save(deliveryLog);
        } catch (Exception e) {
            log.error("Failed to save webhook delivery log", e);
        }
    }

    private String extractField(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (root.hasNonNull(fieldName)) {
                return root.get(fieldName).asText();
            }
        }
        return null;
    }

    private EventSeverity mapSeverity(String severityStr, boolean isRecovery) {
        if (isRecovery) {
            return EventSeverity.LOW;
        }
        if (severityStr == null) {
            return EventSeverity.HIGH;
        }
        return switch (severityStr.toUpperCase()) {
            case "CRITICAL", "P1", "FATAL" -> EventSeverity.CRITICAL;
            case "HIGH", "ERROR", "P2" -> EventSeverity.HIGH;
            case "MEDIUM", "WARN", "WARNING", "P3" -> EventSeverity.MEDIUM;
            default -> EventSeverity.LOW;
        };
    }

    private ConnectedSystem getOrCreateDefaultWebhookSystem(Organization org) {
        List<ConnectedSystem> systems = systemRepository.findByOrganizationId(org.getId());
        for (ConnectedSystem sys : systems) {
            if (sys.getSystemType() == SystemType.WEBHOOK) {
                return sys;
            }
        }
        ConnectedSystem newSys = new ConnectedSystem(
                null,
                org,
                "Webhook Ingestion Service",
                SystemType.WEBHOOK,
                "Auto-created system for inbound webhook event monitoring",
                "PRODUCTION"
        );
        newSys.setStatus(SystemStatus.HEALTHY);
        return systemRepository.save(newSys);
    }
}
