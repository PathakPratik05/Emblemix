package com.emblemix.webhook;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookDeliveryLogRepository extends JpaRepository<WebhookDeliveryLog, UUID> {
    List<WebhookDeliveryLog> findByWebhookIdOrderByReceivedAtDesc(UUID webhookId, Pageable pageable);
    List<WebhookDeliveryLog> findByOrganizationIdOrderByReceivedAtDesc(UUID organizationId, Pageable pageable);
}
