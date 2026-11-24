package com.emblemix.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, UUID> {
    Optional<NotificationChannel> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<NotificationChannel> findByOrganizationIdAndEnabledTrue(UUID organizationId);
    List<NotificationChannel> findByOrganizationId(UUID organizationId);
}
