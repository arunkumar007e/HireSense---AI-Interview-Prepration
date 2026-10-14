package com.hiresense.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        int status,
        String message,
        Map<String, String> fieldErrors,
        Instant timestamp
) {}