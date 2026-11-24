package com.emblemix.system;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConnectedSystemRepository extends JpaRepository<ConnectedSystem, UUID> {
    Optional<ConnectedSystem> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<ConnectedSystem> findByOrganizationId(UUID organizationId);
    Page<ConnectedSystem> findByOrganizationId(UUID organizationId, Pageable pageable);
    Page<ConnectedSystem> findByOrganizationIdAndSystemType(UUID organizationId, SystemType systemType, Pageable pageable);
    List<ConnectedSystem> findByOrganizationIdAndSystemType(UUID organizationId, SystemType systemType);
    List<ConnectedSystem> findByOrganizationIdAndOwningTeamId(UUID organizationId, UUID owningTeamId);
    long countByOrganizationId(UUID organizationId);
    long countByOrganizationIdAndStatus(UUID organizationId, SystemStatus status);
    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}
