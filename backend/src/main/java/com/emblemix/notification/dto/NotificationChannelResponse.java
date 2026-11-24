package com.emblemix.notification.dto;

import com.emblemix.notification.NotificationChannel;
import com.emblemix.notification.NotificationChannelType;

import java.time.Instant;
import java.util.UUID;

public record NotificationChannelResponse(
        UUID id,
        UUID organizationId,
        NotificationChannelType channelType,
        String destination,
        boolean enabled,
        boolean notifyOnCritical,
        boolean notifyOnHigh,
        boolean notifyOnResolve,
        boolean notifyOnAcknowledge,
        Instant createdAt
) {
    public static NotificationChannelResponse from(NotificationChannel channel) {
        return new NotificationChannelResponse(
                channel.getId(),
                channel.getOrganization().getId(),
                channel.getChannelType(),
                channel.getDestination(),
                channel.isEnabled(),
                channel.isNotifyOnCritical(),
                channel.isNotifyOnHigh(),
                channel.isNotifyOnResolve(),
                channel.isNotifyOnAcknowledge(),
                channel.getCreatedAt()
        );
    }
}
