package com.hiresense.security;

import com.hiresense.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final CustomUserDetailsService userDetailsService;

    public CurrentUser(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    public User get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return userDetailsService.loadUserById(principal.getId());
    }
}