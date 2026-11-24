package com.emblemix.monitoring.salesforce.dto;

public record SalesforceLimitsDto(
        long max,
        long remaining,
        double percentageUsed
) {}
