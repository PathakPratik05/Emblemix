package com.emblemix.notification.service;

import com.emblemix.common.response.PagedResponse;
import com.emblemix.notification.dto.NotificationChannelRequest;
import com.emblemix.notification.dto.NotificationChannelResponse;
import com.emblemix.notification.dto.NotificationLogResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing notification channels (Email, Slack, PagerDuty) and delivery logs.
 */
public interface NotificationChannelService {

    /**
     * Creates a new notification dispatch channel for the organization.
     */
    NotificationChannelResponse createChannel(NotificationChannelRequest request);

    /**
     * Lists active and configured notification channels for current tenant.
     */
    List<NotificationChannelResponse> getChannels();

    /**
     * Retrieves channel settings by ID.
     */
    NotificationChannelResponse getChannelById(UUID id);

    /**
     * Updates channel destination, triggers, or enabled status.
     */
    NotificationChannelResponse updateChannel(UUID id, NotificationChannelRequest request);

    /**
     * Deletes a notification channel.
     */
    void deleteChannel(UUID id);

    /**
     * Queries notification delivery audit logs with pagination.
     */
    PagedResponse<NotificationLogResponse> getNotificationLogs(Pageable pageable);
}
