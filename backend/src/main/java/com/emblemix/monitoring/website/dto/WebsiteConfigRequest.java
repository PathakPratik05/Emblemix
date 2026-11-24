package com.emblemix.monitoring.website.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WebsiteConfigRequest(
        @NotBlank(message = "Website name is required")
        @Size(max = 255)
        String name,

        @NotBlank(message = "Website URL is required")
        String url,

        @Min(value = 10, message = "Monitoring interval must be at least 10 seconds")
        @Max(value = 86400, message = "Monitoring interval cannot exceed 24 hours")
        Integer monitoringIntervalSeconds,

        @Min(value = 1000, message = "Request timeout must be at least 1000ms")
        @Max(value = 30000, message = "Request timeout cannot exceed 30000ms")
        Integer requestTimeoutMs,

        @Min(100) @Max(599)
        Integer expectedHttpStatusMin,

        @Min(100) @Max(599)
        Integer expectedHttpStatusMax,

        String expectedResponseContent,

        Boolean checkSsl,

        @Min(1) @Max(365)
        Integer sslExpiryThresholdDays,

        @Min(1) @Max(20)
        Integer consecutiveFailureThreshold,

        String description,

        String environment
) {}
