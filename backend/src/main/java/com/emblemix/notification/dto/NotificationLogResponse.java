package com.emblemix.notification.dto;

import com.emblemix.notification.NotificationChannelType;
import com.emblemix.notification.NotificationLog;
import com.emblemix.notification.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationLogResponse(
        UUID id,
        UUID organizationId,
        UUID incidentId,
        NotificationChannelType channelType,
        String recipient,
        String subject,
        NotificationStatus status,
        int retryCount,
        String errorMessage,
        Instant sentAt,
        Instant createdAt
) {
    public static NotificationLogResponse from(NotificationLog log) {
        return new NotificationLogResponse(
                log.getId(),
                log.getOrganization().getId(),
                log.getIncident().getId(),
                log.getChannelType(),
                log.getRecipient(),
                log.getSubject(),
                log.getStatus(),
                log.getRetryCount(),
                log.getErrorMessage(),
                log.getSentAt(),
                log.getCreatedAt()
        );
    }
}
