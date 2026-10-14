package com.hiresense.service;

import com.hiresense.dto.profile.UserProfileRequest;
import com.hiresense.dto.profile.UserProfileResponse;
import com.hiresense.entity.User;
import com.hiresense.entity.UserProfile;
import com.hiresense.exception.ResourceNotFoundException;
import com.hiresense.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserProfileRepository userProfileRepository;

    public UserService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(User user) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null) {
            return null;
        }
        return toResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(User user, UserProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
        }
        profile.setHeadline(trimToNull(request.headline()));
        profile.setSummary(trimToNull(request.summary()));
        profile.setExperienceYears(request.experienceYears());
        profile.setEducation(trimToNull(request.education()));
        profile.setLocation(trimToNull(request.location()));
        profile.setPhone(trimToNull(request.phone()));
        profile.setGithubUrl(trimToNull(request.githubUrl()));
        profile.setLinkedinUrl(trimToNull(request.linkedinUrl()));
        profile.setPortfolioUrl(trimToNull(request.portfolioUrl()));
        profile = userProfileRepository.save(profile);
        return toResponse(profile);
    }

    private UserProfileResponse toResponse(UserProfile profile) {
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
                profile.getUpdatedAt());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}