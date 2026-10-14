package com.hiresense.dto.resume;

import java.time.Instant;
import java.util.List;

public record ResumeSummaryResponse(
        Long id,
        String fileName,
        String fileType,
        Long fileSize,
        String analysisStatus,
        Integer atsScore,
        Integer keywordMatchScore,
        String analysisSummary,
        List<String> parsedSkills,
        Instant createdAt,
        Instant analyzedAt
) {}