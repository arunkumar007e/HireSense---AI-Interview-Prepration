package com.hiresense.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "answer_evaluations", indexes = {
        @Index(name = "idx_evaluations_answer", columnList = "answer_id"),
        @Index(name = "idx_evaluations_session", columnList = "session_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AnswerEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answer_id", nullable = false, unique = true)
    private InterviewAnswer answer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @Column(nullable = false)
    private Integer score;

    private Integer relevanceScore;

    private Integer clarityScore;

    private Integer depthScore;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(length = 120)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String improvements;

    @Column(columnDefinition = "TEXT")
    private String missingKeywords;

    @Column(columnDefinition = "TEXT")
    private String suggestedAnswer;

    @Column(nullable = false, updatable = false)
    private Instant evaluatedAt;

    @PrePersist
    public void onCreate() {
        this.evaluatedAt = Instant.now();
    }
}