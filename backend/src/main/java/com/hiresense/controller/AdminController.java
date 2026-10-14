package com.hiresense.controller;

import com.hiresense.dto.admin.AdminStatsResponse;
import com.hiresense.dto.admin.UserRoleRequest;
import com.hiresense.dto.auth.UserResponse;
import com.hiresense.security.CurrentUser;
import com.hiresense.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Administration endpoints (role ADMIN required)")
public class AdminController {

    private final AdminService adminService;
    private final CurrentUser currentUser;

    public AdminController(AdminService adminService, CurrentUser currentUser) {
        this.adminService = adminService;
        this.currentUser = currentUser;
    }

    @GetMapping("/stats")
    @Operation(summary = "Platform-wide statistics for the admin dashboard")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    @Operation(summary = "List all registered users")
    public ResponseEntity<List<UserResponse>> getUsers() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @PatchMapping("/users/{userId}/role")
    @Operation(summary = "Change a user's role")
    public ResponseEntity<Void> changeRole(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleRequest request) {
        adminService.changeRole(userId, request.role(), currentUser.get());
        return ResponseEntity.noContent().build();
    }
}