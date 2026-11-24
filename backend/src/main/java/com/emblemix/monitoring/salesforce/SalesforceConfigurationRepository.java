package com.emblemix.monitoring.salesforce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesforceConfigurationRepository extends JpaRepository<SalesforceConfiguration, UUID> {
    Optional<SalesforceConfiguration> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<SalesforceConfiguration> findBySystemIdAndOrganizationId(UUID systemId, UUID organizationId);
    Optional<SalesforceConfiguration> findBySystemId(UUID systemId);
    List<SalesforceConfiguration> findByOrganizationId(UUID organizationId);

    @Query("SELECT s FROM SalesforceConfiguration s WHERE s.system.enabled = true")
    List<SalesforceConfiguration> findAllActiveConfigurations();
}
