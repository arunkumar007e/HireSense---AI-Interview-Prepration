package com.hiresense.dto.admin;

import com.hiresense.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UserRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {}