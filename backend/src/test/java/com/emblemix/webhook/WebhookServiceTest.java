package com.emblemix.webhook;

import com.emblemix.common.exception.UnauthorizedException;
import com.emblemix.common.util.AesEncryptionUtil;
import com.emblemix.event.Event;
import com.emblemix.event.EventIngestionService;
import com.emblemix.event.EventSeverity;
import com.emblemix.event.EventSourceType;
import com.emblemix.event.EventType;
import com.emblemix.organization.Organization;
import com.emblemix.organization.OrganizationRepository;
import com.emblemix.system.ConnectedSystem;
import com.emblemix.system.ConnectedSystemRepository;
import com.emblemix.system.SystemStatus;
import com.emblemix.system.SystemType;
import com.emblemix.webhook.dto.CreateWebhookRequest;
import com.emblemix.webhook.dto.WebhookIngestResult;
import com.emblemix.webhook.dto.WebhookResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private WebhookConfigurationRepository webhookRepository;

    @Mock
    private WebhookDeliveryLogRepository deliveryLogRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ConnectedSystemRepository systemRepository;

    @Mock
    private EventIngestionService eventIngestionService;

    private AesEncryptionUtil aesEncryptionUtil;
    private ObjectMapper objectMapper;
    private WebhookService webhookService;

    private Organization org;
    private ConnectedSystem system;

    @BeforeEach
    void setUp() {
        aesEncryptionUtil = new AesEncryptionUtil("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        objectMapper = new ObjectMapper();
        webhookService = new WebhookService(
                webhookRepository,
                deliveryLogRepository,
                organizationRepository,
                systemRepository,
                eventIngestionService,
                aesEncryptionUtil,
                objectMapper
        );

        org = new Organization(UUID.randomUUID(), "Test Org", "test-org");
        system = new ConnectedSystem(UUID.randomUUID(), org, "Webhook System", SystemType.WEBHOOK, "Webhook integration", "PRODUCTION");
        system.setStatus(SystemStatus.HEALTHY);
    }

    @Test
    @DisplayName("Should correctly verify HMAC-SHA256 signature with and without sha256= prefix")
    void shouldVerifyHmacSha256Signature() {
        String secret = "supersecretkey12345";
        byte[] payload = "{\"status\":\"FAIL\",\"message\":\"Database down\"}".getBytes(StandardCharsets.UTF_8);

        String hexSig = webhookService.computeHmacSha256(secret, payload);

        // Raw hex signature
        assertThat(webhookService.verifySignature(secret, payload, hexSig)).isTrue();

        // With sha256= prefix
        assertThat(webhookService.verifySignature(secret, payload, "sha256=" + hexSig)).isTrue();

        // Case insensitivity
        assertThat(webhookService.verifySignature(secret, payload, "sha256=" + hexSig.toUpperCase())).isTrue();

        // Wrong signature
        assertThat(webhookService.verifySignature(secret, payload, "sha256=invalidhex123")).isFalse();

        // Null signature
        assertThat(webhookService.verifySignature(secret, payload, null)).isFalse();
    }

    @Test
    @DisplayName("Should create webhook, encrypt secret, and return plaintext secret once")
    void shouldCreateWebhook() {
        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));
        when(systemRepository.findByIdAndOrganizationId(system.getId(), org.getId())).thenReturn(Optional.of(system));
        when(webhookRepository.save(any(WebhookConfiguration.class))).thenAnswer(invocation -> {
            WebhookConfiguration config = invocation.getArgument(0);
            config.setId(UUID.randomUUID());
            return config;
        });

        CreateWebhookRequest req = new CreateWebhookRequest("Payment Alert Webhook", "Alerts for payment failures", system.getId());
        WebhookResponse response = webhookService.createWebhook(org.getId(), req, "http://localhost:8080");

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Payment Alert Webhook");
        assertThat(response.secret()).isNotBlank();
        assertThat(response.webhookUrl()).contains("/api/v1/webhooks/receive/");

        verify(webhookRepository).save(any(WebhookConfiguration.class));
    }

    @Test
    @DisplayName("Should reject webhook delivery with invalid HMAC signature")
    void shouldRejectInvalidSignature() {
        UUID webhookId = UUID.randomUUID();
        String plainSecret = "test-secret-12345";
        String encryptedSecret = aesEncryptionUtil.encrypt(plainSecret);

        WebhookConfiguration config = new WebhookConfiguration(
                webhookId, org, system, "Test Hook", "Desc", encryptedSecret, true
        );
        when(webhookRepository.findById(webhookId)).thenReturn(Optional.of(config));

        byte[] payload = "{\"message\":\"Failure detected\"}".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> webhookService.receiveWebhook(webhookId, payload, "sha256=badsignature"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid HMAC signature");

        verify(deliveryLogRepository).save(any(WebhookDeliveryLog.class));
    }

    @Test
    @DisplayName("Should successfully ingest valid signed webhook payload and trigger event ingestion")
    void shouldIngestValidSignedWebhook() {
        UUID webhookId = UUID.randomUUID();
        String plainSecret = "secret-key-abcdef-123456";
        String encryptedSecret = aesEncryptionUtil.encrypt(plainSecret);

        WebhookConfiguration config = new WebhookConfiguration(
                webhookId, org, system, "Billing Webhook", "Desc", encryptedSecret, true
        );
        when(webhookRepository.findById(webhookId)).thenReturn(Optional.of(config));

        Event savedEvent = new Event(
                UUID.randomUUID(), org, system, EventSourceType.WEBHOOK,
                EventType.WEBHOOK_INCIDENT, EventSeverity.HIGH,
                "Payment Gateway Gateway Timeout", "{}", "fingerprint-123", Instant.now()
        );
        when(eventIngestionService.processNormalizedEvent(any(Event.class), any(), any(), any()))
                .thenReturn(savedEvent);

        String jsonPayload = """
                {
                  "title": "Payment Gateway Timeout",
                  "message": "Payment Gateway Gateway Timeout",
                  "severity": "HIGH",
                  "status": "FAIL"
                }
                """;
        byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
        String validSig = "sha256=" + webhookService.computeHmacSha256(plainSecret, payloadBytes);

        WebhookIngestResult result = webhookService.receiveWebhook(webhookId, payloadBytes, validSig);

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.eventId()).isEqualTo(savedEvent.getId());

        verify(eventIngestionService).processNormalizedEvent(any(Event.class), any(), any(), any());
        verify(deliveryLogRepository).save(any(WebhookDeliveryLog.class));
    }
}
