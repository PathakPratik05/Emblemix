package com.emblemix.monitoring.salesforce.dto;

import com.emblemix.monitoring.salesforce.SalesforceConfiguration;
import com.emblemix.system.SystemStatus;

import java.time.Instant;
import java.util.UUID;

public record SalesforceConfigResponse(
        UUID id,
        UUID systemId,
        String name,
        boolean isSandbox,
        double apiUsageWarningThresholdPct,
        double apiUsageCriticalThresholdPct,
        boolean monitorApexJobs,
        boolean monitorLoginActivity,
        String environment,
        SystemStatus status,
        boolean enabled,
        boolean connected,
        String instanceUrl,
        String externalOrgId,
        Instant lastApexJobCheckedAt,
        Instant lastLoginCheckedAt,
        Instant lastMonitoredAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static SalesforceConfigResponse of(SalesforceConfiguration config, boolean connected, String instanceUrl, String externalOrgId) {
        return new SalesforceConfigResponse(
                config.getId(),
                config.getSystem().getId(),
                config.getSystem().getName(),
                config.isSandbox(),
                config.getApiUsageWarningThresholdPct(),
                config.getApiUsageCriticalThresholdPct(),
                config.isMonitorApexJobs(),
                config.isMonitorLoginActivity(),
                config.getSystem().getEnvironment(),
                config.getSystem().getStatus(),
                config.getSystem().isEnabled(),
                connected,
                instanceUrl,
                externalOrgId,
                config.getLastApexJobCheckedAt(),
                config.getLastLoginCheckedAt(),
                config.getSystem().getLastMonitoredAt(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
