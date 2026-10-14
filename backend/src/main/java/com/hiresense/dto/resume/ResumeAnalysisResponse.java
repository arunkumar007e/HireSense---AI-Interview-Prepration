package com.hiresense.dto.resume;

import java.util.List;

public record ResumeAnalysisResponse(
        Long resumeId,
        String fileName,
        int atsScore,
        int keywordMatchScore,
        List<String> extractedSkills,
        String summary,
        List<String> strengths,
        List<String> improvements,
        String status
) {}