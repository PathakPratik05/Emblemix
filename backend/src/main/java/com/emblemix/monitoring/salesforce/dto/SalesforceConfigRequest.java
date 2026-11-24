package com.emblemix.monitoring.salesforce.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SalesforceConfigRequest(
        @NotBlank(message = "System name is required")
        @Size(max = 255)
        String name,

        Boolean isSandbox,

        @DecimalMin("1.0") @DecimalMax("100.0")
        Double apiUsageWarningThresholdPct,

        @DecimalMin("1.0") @DecimalMax("100.0")
        Double apiUsageCriticalThresholdPct,

        Boolean monitorApexJobs,

        Boolean monitorLoginActivity,

        String environment,

        String description
) {}
