package com.hiresense.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "learning_plan_items", indexes = {
        @Index(name = "idx_plan_items_plan", columnList = "plan_id")
})
@Getter
@Setter
@NoArgsConstructor
public class LearningPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private LearningPlan plan;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String resourceUrl;

    @Column(length = 120)
    private String skillName;

    @Column(nullable = false)
    private Integer orderIndex;

    private Integer targetDays;

    @Column(nullable = false)
    private boolean completed = false;

    private Instant completedAt;

    @PrePersist
    public void onCreate() {
        if (this.completedAt == null) {
            this.completed = false;
        }
    }
}