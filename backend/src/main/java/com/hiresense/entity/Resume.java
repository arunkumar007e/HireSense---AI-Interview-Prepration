package com.hiresense.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "resumes", indexes = {
        @Index(name = "idx_resumes_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(length = 100)
    private String fileType;

    private Long fileSize;

    @Column(length = 500)
    private String storageUrl;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String extractedText;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "resume_skills",
            joinColumns = @JoinColumn(name = "resume_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> parsedSkills = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String analysisSummary;

    private Integer atsScore;

    private Integer keywordMatchScore;

    @Column(length = 50)
    private String analysisStatus = "PENDING";

    private Instant analyzedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }
}