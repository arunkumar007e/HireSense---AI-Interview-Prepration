package com.hiresense.repository;

import com.hiresense.entity.Progress;
import com.hiresense.enums.ProgressMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, Long> {

    List<Progress> findByUserIdOrderByRecordedAtDesc(Long userId);

    List<Progress> findByUserIdAndMetricTypeOrderByRecordedAtDesc(Long userId, ProgressMetric metricType);
}