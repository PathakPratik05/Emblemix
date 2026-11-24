package com.emblemix.notification.dto;

import com.emblemix.notification.NotificationChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationChannelRequest(
        @NotNull(message = "Channel type is required")
        NotificationChannelType channelType,

        @NotBlank(message = "Destination is required")
        @Size(max = 255)
        String destination,

        Boolean enabled,
        Boolean notifyOnCritical,
        Boolean notifyOnHigh,
        Boolean notifyOnResolve,
        Boolean notifyOnAcknowledge
) {}
