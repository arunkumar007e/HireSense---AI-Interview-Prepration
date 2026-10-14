package com.hiresense.repository;

import com.hiresense.entity.InterviewSession;
import com.hiresense.enums.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    List<InterviewSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<InterviewSession> findByIdAndUserId(Long id, Long userId);

    List<InterviewSession> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, InterviewStatus status);

    long countByUserId(Long userId);

    long countByStatus(InterviewStatus status);

    @Query("select count(q) from InterviewQuestion q join q.session s")
    long totalAnsweredQuestions();

    @Query("select count(q) from InterviewQuestion q join q.session s where q.answer is not null")
    long totalAnsweredQuestionsByAnswer();
}