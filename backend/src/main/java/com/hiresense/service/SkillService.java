package com.hiresense.service;

import com.hiresense.dto.skill.JobRoleResponse;
import com.hiresense.dto.skill.SkillGapItemResponse;
import com.hiresense.dto.skill.SkillGapResponse;
import com.hiresense.dto.skill.SkillResponse;
import com.hiresense.entity.JobRole;
import com.hiresense.entity.Skill;
import com.hiresense.enums.SkillProficiency;
import com.hiresense.exception.ResourceNotFoundException;
import com.hiresense.repository.JobRoleRepository;
import com.hiresense.repository.SkillRepository;
import com.hiresense.service.ai.AiService;
import com.hiresense.service.ai.dto.AiSkillGap;
import com.hiresense.service.ai.dto.AiSkillGapItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SkillService {

    private final SkillRepository skillRepository;
    private final JobRoleRepository jobRoleRepository;
    private final AiService aiService;

    public SkillService(SkillRepository skillRepository,
                        JobRoleRepository jobRoleRepository,
                        AiService aiService) {
        this.skillRepository = skillRepository;
        this.jobRoleRepository = jobRoleRepository;
        this.aiService = aiService;
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getAllSkills() {
        return skillRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toSkillResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JobRoleResponse> getAllJobRoles() {
        return jobRoleRepository.findAllByOrderByTitleAsc()
                .stream()
                .map(this::toJobRoleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobRoleResponse getJobRole(Long id) {
        JobRole role = jobRoleRepository.findWithSkillsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job role not found with id " + id));
        return toJobRoleResponse(role);
    }

    @Transactional
    public SkillGapResponse analyzeSkillGap(Long jobRoleId, List<String> resumeSkills) {
        JobRole role = jobRoleRepository.findWithSkillsById(jobRoleId)
                .orElseThrow(() -> new ResourceNotFoundException("Job role not found with id " + jobRoleId));

        List<String> requiredNames = role.getRequiredSkills().stream()
                .map(Skill::getName)
                .toList();

        AiSkillGap aiGap = aiService.analyzeSkillGap(resumeSkills, requiredNames, role.getTitle());

        List<SkillGapItemResponse> items = new ArrayList<>();
        if (aiGap.skills() != null) {
            for (AiSkillGapItem item : aiGap.skills()) {
                items.add(new SkillGapItemResponse(
                        item.skill(),
                        parseProficiency(item.requiredLevel(), SkillProficiency.ADVANCED),
                        parseProficiency(item.currentLevel(), SkillProficiency.BEGINNER),
                        item.gap(),
                        item.explanation()));
            }
        }
        return new SkillGapResponse(
                role.getId(),
                role.getTitle(),
                clamp(aiGap.matchScore()),
                aiGap.summary(),
                items);
    }

    @Transactional
    public Set<Skill> ensureSkills(List<String> names) {
        Set<Skill> result = new java.util.LinkedHashSet<>();
        if (names == null) {
            return result;
        }
        for (String raw : names) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String normalized = raw.trim();
            Skill skill = skillRepository.findByNameIgnoreCase(normalized).orElseGet(() -> {
                Skill created = new Skill();
                created.setName(normalized);
                created.setCategory("Custom");
                return skillRepository.save(created);
            });
            result.add(skill);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<String> matchSkillsByName(List<String> names) {
        if (names == null || names.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> normalized = names.stream()
                .filter(n -> n != null && !n.isBlank())
                .map(String::trim)
                .toList();
        return skillRepository.findByNameIgnoreCaseIn(normalized)
                .stream()
                .map(Skill::getName)
                .toList();
    }

    private SkillProficiency parseProficiency(String value, SkillProficiency fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return SkillProficiency.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private SkillResponse toSkillResponse(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getName(), skill.getCategory(), skill.getDescription());
    }

    private JobRoleResponse toJobRoleResponse(JobRole role) {
        List<SkillResponse> skills = role.getRequiredSkills().stream()
                .map(this::toSkillResponse)
                .toList();
        return new JobRoleResponse(role.getId(), role.getTitle(), role.getDescription(), role.getCategory(), skills);
    }
}