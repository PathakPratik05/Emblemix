package com.emblemix.notification.service;

import com.emblemix.incident.Incident;

/**
 * Service interface for asynchronous incident alerting and multi-channel notification dispatching.
 */
public interface NotificationService {

    /**
     * Evaluates channel notification preferences and dispatches alerts asynchronously.
     */
    void dispatchIncidentNotifications(Incident incident, String action);
}
