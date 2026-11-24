package com.emblemix.incident;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    Optional<Incident> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Page<Incident> findByOrganizationId(UUID organizationId, Pageable pageable);
    Page<Incident> findByOrganizationIdAndStatus(UUID organizationId, IncidentStatus status, Pageable pageable);
    Page<Incident> findByOrganizationIdAndSystemId(UUID organizationId, UUID systemId, Pageable pageable);
    Page<Incident> findByOrganizationIdAndSeverity(UUID organizationId, IncidentSeverity severity, Pageable pageable);

    Optional<Incident> findFirstByOrganizationIdAndFingerprintAndStatusIn(
            UUID organizationId,
            String fingerprint,
            Collection<IncidentStatus> statuses
    );

    List<Incident> findByOrganizationIdAndFingerprintAndStatusIn(
            UUID organizationId,
            String fingerprint,
            Collection<IncidentStatus> statuses
    );

    long countByOrganizationId(UUID organizationId);
    long countByOrganizationIdAndStatus(UUID organizationId, IncidentStatus status);
    long countByOrganizationIdAndSeverity(UUID organizationId, IncidentSeverity severity);
    long countByOrganizationIdAndResolvedAtAfter(UUID organizationId, Instant after);

    @Query("SELECT i FROM Incident i WHERE i.organization.id = :orgId AND i.resolvedAt IS NOT NULL AND i.resolvedAt >= :since")
    List<Incident> findResolvedIncidentsSince(@Param("orgId") UUID orgId, @Param("since") Instant since);
}
