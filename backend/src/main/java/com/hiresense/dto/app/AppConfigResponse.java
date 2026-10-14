package com.hiresense.dto.app;

public record AppConfigResponse(
        String aiProvider,
        boolean aiConfigured
) {}