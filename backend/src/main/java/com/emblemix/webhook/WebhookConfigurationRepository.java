package com.emblemix.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookConfigurationRepository extends JpaRepository<WebhookConfiguration, UUID> {
    List<WebhookConfiguration> findByOrganizationId(UUID organizationId);
    Optional<WebhookConfiguration> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<WebhookConfiguration> findBySystemId(UUID systemId);
}
