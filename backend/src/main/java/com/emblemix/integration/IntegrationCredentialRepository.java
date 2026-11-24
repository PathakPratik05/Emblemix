package com.emblemix.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationCredentialRepository extends JpaRepository<IntegrationCredential, UUID> {
    Optional<IntegrationCredential> findBySystemIdAndIntegrationType(UUID systemId, IntegrationType integrationType);
    Optional<IntegrationCredential> findBySystemIdAndOrganizationId(UUID systemId, UUID organizationId);
    void deleteBySystemId(UUID systemId);
}
