package com.hiresense.dto.auth;

import com.hiresense.dto.profile.UserProfileResponse;

import java.time.Instant;

public record AuthResponse(
        String token,
        String tokenType,
        String email,
        String fullName,
        String role,
        Long userId,
        Instant expiresAt,
        UserProfileResponse profile
) {}