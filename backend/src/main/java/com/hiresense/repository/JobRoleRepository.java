package com.hiresense.repository;

import com.hiresense.entity.JobRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRoleRepository extends JpaRepository<JobRole, Long> {

    @EntityGraph(attributePaths = "requiredSkills")
    List<JobRole> findAllByOrderByTitleAsc();

    @EntityGraph(attributePaths = "requiredSkills")
    Optional<JobRole> findWithSkillsById(Long id);

    Optional<JobRole> findByTitleIgnoreCase(String title);
}