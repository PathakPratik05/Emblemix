package com.emblemix.alert;

import com.emblemix.event.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {
    Optional<AlertRule> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<AlertRule> findByOrganizationId(UUID organizationId);

    @Query("SELECT r FROM AlertRule r WHERE r.organization.id = :orgId AND r.enabled = true AND r.eventType = :eventType AND (r.system IS NULL OR r.system.id = :systemId)")
    List<AlertRule> findApplicableRules(
            @Param("orgId") UUID orgId,
            @Param("eventType") EventType eventType,
            @Param("systemId") UUID systemId
    );
}
