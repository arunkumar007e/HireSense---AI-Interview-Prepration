package com.hiresense.dto.progress;

import com.hiresense.enums.ProgressMetric;

import java.time.Instant;

public record ProgressResponse(
        Long id,
        ProgressMetric metricType,
        int metricValue,
        String relatedTitle,
        String note,
        Instant recordedAt
) {}