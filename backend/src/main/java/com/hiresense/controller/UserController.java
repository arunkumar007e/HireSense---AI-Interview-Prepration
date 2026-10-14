package com.hiresense.controller;

import com.hiresense.dto.profile.UserProfileRequest;
import com.hiresense.dto.profile.UserProfileResponse;
import com.hiresense.security.CurrentUser;
import com.hiresense.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/profile")
@Tag(name = "User Profile", description = "Manage the candidate profile")
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    public UserController(UserService userService, CurrentUser currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "Get the current user profile")
    public ResponseEntity<UserProfileResponse> getProfile() {
        return ResponseEntity.ok(userService.getProfile(currentUser.get()));
    }

    @PutMapping
    @Operation(summary = "Create or update the current user profile")
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(currentUser.get(), request));
    }
}