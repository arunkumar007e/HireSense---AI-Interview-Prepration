package com.hiresense.repository;

import com.hiresense.entity.AnswerEvaluation;
import com.hiresense.entity.InterviewSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerEvaluationRepository extends JpaRepository<AnswerEvaluation, Long> {

    List<AnswerEvaluation> findBySessionId(Long sessionId);

    @EntityGraph(attributePaths = "answer")
    List<AnswerEvaluation> findBySession(InterviewSession session);
}