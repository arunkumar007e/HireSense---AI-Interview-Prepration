package com.hiresense.entity;

import com.hiresense.enums.ProgressMetric;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "progress_entries", indexes = {
        @Index(name = "idx_progress_user", columnList = "user_id"),
        @Index(name = "idx_progress_user_time", columnList = "user_id, recorded_at"),
        @Index(name = "idx_progress_metric", columnList = "metric_type")
})
@Getter
@Setter
@NoArgsConstructor
public class Progress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProgressMetric metricType;

    @Column(nullable = false)
    private Integer metricValue;

    @Column(length = 200)
    private String relatedTitle;

    private Long relatedId;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false, updatable = false)
    private Instant recordedAt;

    @PrePersist
    public void onCreate() {
        this.recordedAt = Instant.now();
    }
}