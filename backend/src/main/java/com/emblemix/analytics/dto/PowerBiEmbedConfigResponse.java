package com.emblemix.analytics.dto;

import java.time.Instant;

public record PowerBiEmbedConfigResponse(
        String reportId,
        String reportName,
        String embedUrl,
        String embedToken,
        Instant tokenExpiration,
        String workspaceId,
        String entraTenantId,
        String organizationId
) {}
