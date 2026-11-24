package com.emblemix.monitoring.salesforce.dto;

import java.util.List;

public record SalesforceTestResultDto(
        boolean connected,
        String instanceUrl,
        String externalOrgId,
        SalesforceLimitsDto limits,
        List<SalesforceApexJobDto> failedApexJobs,
        String message,
        String error
) {}
