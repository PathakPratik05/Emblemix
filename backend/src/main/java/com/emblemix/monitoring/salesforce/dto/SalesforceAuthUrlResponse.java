package com.emblemix.monitoring.salesforce.dto;

public record SalesforceAuthUrlResponse(
        String authorizationUrl,
        String state
) {}
