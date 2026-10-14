package com.hiresense.entity;

import com.hiresense.enums.DifficultyLevel;
import com.hiresense.enums.InterviewStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interview_sessions", indexes = {
        @Index(name = "idx_sessions_user", columnList = "user_id"),
        @Index(name = "idx_sessions_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_role_id")
    private JobRole jobRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewStatus status = InterviewStatus.CONFIGURED;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DifficultyLevel difficultyLevel = DifficultyLevel.INTERMEDIATE;

    @Column(nullable = false)
    private Integer numberOfQuestions = 5;

    private Integer timeLimitSeconds;

    private Integer currentQuestionIndex = 0;

    private Instant startedAt;

    private Instant completedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InterviewQuestion> questions = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }
}