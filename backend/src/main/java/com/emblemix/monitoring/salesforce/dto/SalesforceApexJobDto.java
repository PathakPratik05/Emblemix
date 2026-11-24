package com.emblemix.monitoring.salesforce.dto;

import java.time.Instant;

public record SalesforceApexJobDto(
        String id,
        String jobType,
        String status,
        int numberOfErrors,
        String extendedStatus,
        Instant createdDate
) {}
