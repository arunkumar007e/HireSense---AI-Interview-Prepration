package com.hiresense.service.ai;

import java.util.List;
import java.util.Set;

/**
 * Static catalogue of commonly used technical and professional skills used by
 * the mock AI provider for extraction and matching. The real provider extracts
 * skills from the resume directly.
 */
final class SkillDictionary {

    static final List<String> NAMES = List.of(
            "Java", "Spring Boot", "Spring", "Hibernate", "REST APIs", "Microservices",
            "SQL", "PostgreSQL", "MySQL", "MongoDB", "Redis", "Kafka", "Docker", "Kubernetes",
            "AWS", "Azure", "GCP", "CI/CD", "Git", "Maven", "Gradle", "JUnit", "TDD",
            "JavaScript", "TypeScript", "React", "Angular", "Vue", "Node.js", "HTML", "CSS",
            "Python", "Django", "Flask", "FastAPI", "Pandas", "NumPy", "Machine Learning",
            "Deep Learning", "TensorFlow", "PyTorch", "Scikit-learn", "NLP", "Data Science",
            "Data Analysis", "Go", "Rust", "C#", ".NET", "PHP", "Laravel", "Ruby", "Rails",
            "GraphQL", "gRPC", "System Design", "Algorithms", "Data Structures", "OOP",
            "Design Patterns", "Linux", "Shell Scripting", "Networking", "DevOps",
            "Agile", "Scrum", "Kanban", "Communication", "Leadership", "Teamwork",
            "Problem Solving", "Analytical Thinking", "Project Management", "Product Management",
            "Selenium", "Playwright", "Cypress", "Automation Testing", "Security",
            "Firebase", "Flutter", "Swift", "Kotlin", "Android", "iOS", "Performance Testing"
    );

    static final Set<String> SOFT_SKILLS = Set.of(
            "Communication", "Leadership", "Teamwork", "Problem Solving",
            "Analytical Thinking", "Project Management", "Adaptability", "Collaboration"
    );

    private SkillDictionary() {
    }
}