package com.emblemix.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    Optional<Event> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Page<Event> findByOrganizationId(UUID organizationId, Pageable pageable);
    Page<Event> findByOrganizationIdAndSystemId(UUID organizationId, UUID systemId, Pageable pageable);
    List<Event> findTop10ByOrganizationIdOrderByOccurredAtDesc(UUID organizationId);
    boolean existsByFingerprintAndOccurredAtAfter(String fingerprint, Instant after);
    long countByOrganizationIdAndOccurredAtAfter(UUID organizationId, Instant after);
}
