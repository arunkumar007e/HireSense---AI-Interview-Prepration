package com.hiresense.repository;

import com.hiresense.entity.LearningPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningPlanRepository extends JpaRepository<LearningPlan, Long> {

    List<LearningPlan> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<LearningPlan> findByIdAndUserId(Long id, Long userId);
}