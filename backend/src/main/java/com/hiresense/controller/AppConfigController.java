package com.hiresense.controller;

import com.hiresense.dto.app.AppConfigResponse;
import com.hiresense.service.ai.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
@Tag(name = "App Config", description = "Public application configuration")
public class AppConfigController {

    private final AiService aiService;

    public AppConfigController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping
    @Operation(summary = "Get public application configuration")
    public AppConfigResponse config() {
        boolean configured = !"mock".equalsIgnoreCase(aiService.providerName());
        return new AppConfigResponse(aiService.providerName(), configured);
    }
}