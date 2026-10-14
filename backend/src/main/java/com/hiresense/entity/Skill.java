package com.hiresense.entity;

import com.hiresense.enums.SkillProficiency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "skills", indexes = {
        @Index(name = "idx_skills_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(length = 120)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "typical_proficiency", length = 20)
    private SkillProficiency typicalProficiency;

    @PrePersist
    @PreUpdate
    public void normalizeName() {
        if (name != null) {
            name = name.trim();
        }
    }
}