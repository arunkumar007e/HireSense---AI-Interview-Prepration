package com.hiresense.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "interview_answers", indexes = {
        @Index(name = "idx_answers_session", columnList = "session_id")
})
@Getter
@Setter
@NoArgsConstructor
public class InterviewAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private InterviewQuestion question;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    private Integer timeTakenSeconds;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    @OneToOne(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AnswerEvaluation evaluation;

    @PrePersist
    public void onCreate() {
        this.submittedAt = Instant.now();
    }
}