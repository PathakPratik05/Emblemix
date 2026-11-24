package com.emblemix.monitoring.website;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MonitoringResultRepository extends JpaRepository<MonitoringResult, UUID> {
    Page<MonitoringResult> findBySystemIdAndOrganizationIdOrderByCheckedAtDesc(
            UUID systemId, UUID organizationId, Pageable pageable
    );

    List<MonitoringResult> findTop50BySystemIdOrderByCheckedAtDesc(UUID systemId);

    @Query("SELECT AVG(m.responseTimeMs) FROM MonitoringResult m WHERE m.organization.id = :orgId AND m.checkedAt >= :since AND m.status = 'SUCCESS'")
    Double getAverageResponseTimeSince(@Param("orgId") UUID orgId, @Param("since") Instant since);

    @Query("SELECT COUNT(m) FROM MonitoringResult m WHERE m.organization.id = :orgId AND m.checkedAt >= :since")
    long countTotalChecksSince(@Param("orgId") UUID orgId, @Param("since") Instant since);

    @Query("SELECT COUNT(m) FROM MonitoringResult m WHERE m.organization.id = :orgId AND m.checkedAt >= :since AND m.status = 'SUCCESS'")
    long countSuccessfulChecksSince(@Param("orgId") UUID orgId, @Param("since") Instant since);

    @Query("SELECT m FROM MonitoringResult m WHERE m.organization.id = :orgId AND m.checkedAt >= :since ORDER BY m.checkedAt ASC")
    List<MonitoringResult> findResultsSince(@Param("orgId") UUID orgId, @Param("since") Instant since);
}
