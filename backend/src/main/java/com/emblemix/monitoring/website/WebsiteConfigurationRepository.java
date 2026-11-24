package com.emblemix.monitoring.website;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebsiteConfigurationRepository extends JpaRepository<WebsiteConfiguration, UUID> {
    Optional<WebsiteConfiguration> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<WebsiteConfiguration> findBySystemIdAndOrganizationId(UUID systemId, UUID organizationId);
    Optional<WebsiteConfiguration> findBySystemId(UUID systemId);
    Page<WebsiteConfiguration> findByOrganizationId(UUID organizationId, Pageable pageable);

    @Query("SELECT w FROM WebsiteConfiguration w JOIN FETCH w.system JOIN FETCH w.organization WHERE w.system.enabled = true")
    List<WebsiteConfiguration> findAllActiveConfigurations();
}
