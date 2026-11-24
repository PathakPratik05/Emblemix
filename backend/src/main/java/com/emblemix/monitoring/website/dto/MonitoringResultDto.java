package com.emblemix.monitoring.website.dto;

import com.emblemix.monitoring.website.MonitoringResult;

import java.time.Instant;
import java.util.UUID;

public record MonitoringResultDto(
        UUID id,
        UUID systemId,
        String status,
        Integer httpStatusCode,
        Long responseTimeMs,
        Boolean sslValid,
        Integer sslDaysRemaining,
        String errorMessage,
        Instant checkedAt
) {
    public static MonitoringResultDto from(MonitoringResult result) {
        return new MonitoringResultDto(
                result.getId(),
                result.getSystem().getId(),
                result.getStatus(),
                result.getHttpStatusCode(),
                result.getResponseTimeMs(),
                result.getSslValid(),
                result.getSslDaysRemaining(),
                result.getErrorMessage(),
                result.getCheckedAt()
        );
    }
}
