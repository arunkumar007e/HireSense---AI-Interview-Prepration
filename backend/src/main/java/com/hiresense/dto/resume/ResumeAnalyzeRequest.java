package com.hiresense.dto.resume;

import jakarta.validation.constraints.NotNull;

public record ResumeAnalyzeRequest(
        @NotNull(message = "Job role id is required for analysis")
        Long jobRoleId
) {}