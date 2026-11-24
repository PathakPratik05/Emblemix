package com.emblemix.analytics.dto;

import java.time.Instant;

public record PowerBiReportCatalogItem(
        String id,
        String name,
        String description,
        String category,
        String workspaceName,
        String datasetRefreshStatus,
        Instant lastRefreshedAt
) {}
