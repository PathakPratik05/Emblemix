package com.emblemix.notification.service.impl;

import com.emblemix.incident.Incident;
import com.emblemix.incident.IncidentSeverity;
import com.emblemix.incident.IncidentStatus;
import com.emblemix.notification.*;
import com.emblemix.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationChannelRepository channelRepository;
    private final NotificationLogRepository logRepository;
    private final JavaMailSender mailSender;

    public NotificationServiceImpl(NotificationChannelRepository channelRepository,
                                   NotificationLogRepository logRepository,
                                   JavaMailSender mailSender) {
        this.channelRepository = channelRepository;
        this.logRepository = logRepository;
        this.mailSender = mailSender;
    }

    @Override
    @Async
    @Transactional
    public void dispatchIncidentNotifications(Incident incident, String action) {
        try {
            List<NotificationChannel> channels = channelRepository
                    .findByOrganizationIdAndEnabledTrue(incident.getOrganization().getId());

            for (NotificationChannel channel : channels) {
                if (shouldNotify(channel, incident, action)) {
                    sendNotificationWithRetry(channel, incident, action);
                }
            }
        } catch (Exception e) {
            log.error("Failed to evaluate notification dispatch for incident {}: {}", incident.getId(), e.getMessage());
        }
    }

    private boolean shouldNotify(NotificationChannel channel, Incident incident, String action) {
        if ("CREATED".equals(action)) {
            if (incident.getSeverity() == IncidentSeverity.CRITICAL && channel.isNotifyOnCritical()) {
                return true;
            }
            if (incident.getSeverity() == IncidentSeverity.HIGH && channel.isNotifyOnHigh()) {
                return true;
            }
        } else if ("ACKNOWLEDGED".equals(action) && channel.isNotifyOnAcknowledge()) {
            return true;
        } else if ("RESOLVED".equals(action) && channel.isNotifyOnResolve()) {
            return true;
        } else if ("ESCALATED".equals(action) || "TEAM_ASSIGNED".equals(action)) {
            return channel.isNotifyOnCritical() || channel.isNotifyOnHigh();
        }
        return false;
    }

    private void sendNotificationWithRetry(NotificationChannel channel, Incident incident, String action) {
        String subject = String.format("[Emblemix Alert] [%s] %s: %s",
                incident.getSeverity(), action, incident.getTitle());
        String body = String.format("""
                Emblemix Incident Notification
                ==============================
                System: %s
                Severity: %s
                Status: %s
                Action: %s
                Description: %s
                Occurred: %s
                """,
                incident.getSystem().getName(),
                incident.getSeverity(),
                incident.getStatus(),
                action,
                incident.getDescription() != null ? incident.getDescription() : "N/A",
                incident.getCreatedAt()
        );

        NotificationLog nLog = new NotificationLog(
                null,
                incident.getOrganization(),
                incident,
                channel.getChannelType(),
                channel.getDestination(),
                subject
        );
        nLog = logRepository.save(nLog);

        int maxRetries = 3;
        boolean sent = false;
        String lastError = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            nLog.setRetryCount(attempt);
            try {
                if (channel.getChannelType() == NotificationChannelType.EMAIL) {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(channel.getDestination());
                    message.setSubject(subject);
                    message.setText(body);
                    mailSender.send(message);
                }
                sent = true;
                break;
            } catch (Exception e) {
                lastError = e.getMessage();
                log.warn("Notification delivery attempt {} failed to {}: {}", attempt, channel.getDestination(), e.getMessage());
                try {
                    Thread.sleep(100L * attempt);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (sent) {
            nLog.setStatus(NotificationStatus.SENT);
            nLog.setSentAt(Instant.now());
            nLog.setErrorMessage(null);
            log.info("Notification successfully sent to {}", channel.getDestination());
        } else {
            nLog.setStatus(NotificationStatus.FAILED);
            nLog.setErrorMessage(lastError);
            log.error("All notification attempts failed to {}: {}", channel.getDestination(), lastError);
        }
        logRepository.save(nLog);
    }
}
