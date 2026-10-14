package com.hiresense.repository;

import com.hiresense.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    List<InterviewQuestion> findBySessionIdOrderByOrderIndexAsc(Long sessionId);

    long countBySessionIdAndAnswerIsNotNull(Long sessionId);
}