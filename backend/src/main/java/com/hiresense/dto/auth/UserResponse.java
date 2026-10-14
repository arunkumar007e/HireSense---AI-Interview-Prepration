package com.hiresense.dto.auth;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String role,
        boolean enabled,
        Instant createdAt
) {}