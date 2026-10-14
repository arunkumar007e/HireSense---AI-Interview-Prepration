package com.hiresense.dto.profile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UserProfileRequest(
        @Size(max = 200, message = "Headline must be at most 200 characters")
        String headline,

        @Size(max = 5000, message = "Summary must be at most 5000 characters")
        String summary,

        @Min(value = 0, message = "Experience years must be >= 0")
        @Max(value = 60, message = "Experience years must be <= 60")
        Integer experienceYears,

        @Size(max = 200, message = "Education must be at most 200 characters")
        String education,

        @Size(max = 200, message = "Location must be at most 200 characters")
        String location,

        @Size(max = 100, message = "Phone must be at most 100 characters")
        String phone,

        @Size(max = 500, message = "GitHub URL must be at most 500 characters")
        String githubUrl,

        @Size(max = 500, message = "LinkedIn URL must be at most 500 characters")
        String linkedinUrl,

        @Size(max = 500, message = "Portfolio URL must be at most 500 characters")
        String portfolioUrl
) {}