package com.emblemix.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    List<NotificationLog> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
    Page<NotificationLog> findByOrganizationId(UUID organizationId, Pageable pageable);
}
