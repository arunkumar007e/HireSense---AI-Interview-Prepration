package com.hiresense.dto.profile;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String headline,
        String summary,
        Integer experienceYears,
        String education,
        String location,
        String phone,
        String githubUrl,
        String linkedinUrl,
        String portfolioUrl,
        Instant updatedAt
) {}