package com.emblemix.reporting.dto;

import java.util.List;

public record IncidentTrendDto(
        String date,
        long count,
        long resolved
) {}
