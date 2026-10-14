package com.hiresense.service;

import com.hiresense.dto.auth.AuthResponse;
import com.hiresense.dto.auth.LoginRequest;
import com.hiresense.dto.auth.RegisterRequest;
import com.hiresense.dto.auth.UserResponse;
import com.hiresense.dto.profile.UserProfileResponse;
import com.hiresense.entity.User;
import com.hiresense.entity.UserProfile;
import com.hiresense.enums.Role;
import com.hiresense.exception.ConflictException;
import com.hiresense.exception.ResourceNotFoundException;
import com.hiresense.repository.UserProfileRepository;
import com.hiresense.repository.UserRepository;
import com.hiresense.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       UserProfileRepository userProfileRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setFullName(request.fullName().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.CANDIDATE);
        user.setEnabled(true);
        user = userRepository.save(user);
        return toAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        return new AuthResponse(
                token,
                "Bearer",
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getId(),
                jwtService.extractExpiration(token),
                profile == null ? null : toProfileResponse(profile));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.isEnabled(),
                user.getCreatedAt());
    }

    private UserProfileResponse toProfileResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getHeadline(),
                profile.getSummary(),
                profile.getExperienceYears(),
                profile.getEducation(),
                profile.getLocation(),
                profile.getPhone(),
                profile.getGithubUrl(),
                profile.getLinkedinUrl(),
                profile.getPortfolioUrl(),
                profile.getUpdatedAt() == null ? Instant.now() : profile.getUpdatedAt());
    }
}