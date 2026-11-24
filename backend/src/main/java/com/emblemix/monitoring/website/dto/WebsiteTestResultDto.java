package com.emblemix.monitoring.website.dto;

public record WebsiteTestResultDto(
        boolean healthy,
        Integer httpStatusCode,
        Long responseTimeMs,
        Boolean sslValid,
        Integer sslDaysRemaining,
        String message,
        String error
) {}
