package com.emblemix.monitoring.website.dto;

import com.emblemix.monitoring.website.WebsiteConfiguration;
import com.emblemix.system.SystemStatus;

import java.time.Instant;
import java.util.UUID;

public record WebsiteConfigResponse(
        UUID id,
        UUID systemId,
        String name,
        String url,
        String environment,
        SystemStatus status,
        boolean enabled,
        int monitoringIntervalSeconds,
        int requestTimeoutMs,
        int expectedHttpStatusMin,
        int expectedHttpStatusMax,
        String expectedResponseContent,
        boolean checkSsl,
        int sslExpiryThresholdDays,
        int consecutiveFailureThreshold,
        int currentConsecutiveFailures,
        Instant lastMonitoredAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static WebsiteConfigResponse from(WebsiteConfiguration config) {
        return new WebsiteConfigResponse(
                config.getId(),
                config.getSystem().getId(),
                config.getSystem().getName(),
                config.getUrl(),
                config.getSystem().getEnvironment(),
                config.getSystem().getStatus(),
                config.getSystem().isEnabled(),
                config.getMonitoringIntervalSeconds(),
                config.getRequestTimeoutMs(),
                config.getExpectedHttpStatusMin(),
                config.getExpectedHttpStatusMax(),
                config.getExpectedResponseContent(),
                config.isCheckSsl(),
                config.getSslExpiryThresholdDays(),
                config.getConsecutiveFailureThreshold(),
                config.getCurrentConsecutiveFailures(),
                config.getSystem().getLastMonitoredAt(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
