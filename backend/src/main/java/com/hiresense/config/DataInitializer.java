package com.hiresense.config;

import com.hiresense.entity.JobRole;
import com.hiresense.entity.Skill;
import com.hiresense.entity.User;
import com.hiresense.enums.Role;
import com.hiresense.enums.SkillProficiency;
import com.hiresense.repository.JobRoleRepository;
import com.hiresense.repository.SkillRepository;
import com.hiresense.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final JobRoleRepository jobRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminName;

    public DataInitializer(UserRepository userRepository,
                           SkillRepository skillRepository,
                           JobRoleRepository jobRoleRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.admin.email}") String adminEmail,
                           @Value("${app.admin.password}") String adminPassword,
                           @Value("${app.admin.name}") String adminName) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.jobRoleRepository = jobRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminName = adminName;
    }

    @Override
    @Transactional
    public void run(String... args) {
        createAdmin();
        seedSkillsAndRoles();
    }

    private void createAdmin() {
        if (userRepository.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }
        User admin = new User();
        admin.setEmail(adminEmail.toLowerCase());
        admin.setFullName(adminName);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);
        log.info("Created default admin user {}", adminEmail);
    }

    private void seedSkillsAndRoles() {
        if (jobRoleRepository.count() > 0) {
            return;
        }
        Map<String, Skill> skillMap = new LinkedHashMap<>();
        for (String name : SEED_SKILLS) {
            skillMap.put(name, ensureSkill(skillMap, name));
        }

        seedRole(skillMap, "Java Backend Developer", "Backend",
                "Build robust server-side applications using the Java platform and modern frameworks.",
                List.of("Java", "Spring Boot", "Spring", "Hibernate", "REST APIs", "Microservices",
                        "SQL", "PostgreSQL", "Docker", "Git", "Maven", "JUnit", "System Design",
                        "Algorithms", "Data Structures", "OOP", "Design Patterns", "CI/CD", "Kafka"));

        seedRole(skillMap, "Full Stack Developer", "Web",
                "Develop end-to-end features spanning databases, APIs, and user interfaces.",
                List.of("JavaScript", "TypeScript", "React", "Node.js", "Java", "Spring Boot",
                        "REST APIs", "SQL", "PostgreSQL", "MongoDB", "HTML", "CSS", "Git",
                        "System Design", "Docker", "CI/CD", "Problem Solving"));

        seedRole(skillMap, "Frontend Developer", "Web",
                "Create responsive, accessible and high-performance user interfaces.",
                List.of("JavaScript", "TypeScript", "React", "HTML", "CSS", "REST APIs",
                        "GraphQL", "Git", "Problem Solving", "Communication"));

        seedRole(skillMap, "DevOps Engineer", "Infrastructure",
                "Automate, containerize and operate cloud infrastructure and CI/CD pipelines.",
                List.of("Docker", "Kubernetes", "AWS", "Azure", "GCP", "CI/CD", "Linux",
                        "Shell Scripting", "Networking", "Git", "Python", "Security", "Maven", "Gradle"));

        seedRole(skillMap, "Data Scientist", "Data",
                "Extract insights from data using statistics, machine learning and visualization.",
                List.of("Python", "Pandas", "NumPy", "Machine Learning", "Deep Learning",
                        "TensorFlow", "PyTorch", "Scikit-learn", "NLP", "Data Analysis",
                        "SQL", "Statistics", "Communication", "Problem Solving"));

        seedRole(skillMap, "Data Engineer", "Data",
                "Design and maintain reliable data pipelines and storage systems.",
                List.of("Python", "SQL", "PostgreSQL", "MongoDB", "Kafka", "AWS", "Docker",
                        "Data Analysis", "Spark", "Linux", "Git", "Problem Solving"));

        seedRole(skillMap, "QA Automation Engineer", "Quality",
                "Automate end-to-end test suites and integrate them into CI/CD.",
                List.of("Java", "Python", "Automation Testing", "Selenium", "Cypress",
                        "Playwright", "JUnit", "CI/CD", "Git", "Problem Solving"));

        log.info("Seeded {} skills and {} job roles", skillRepository.count(), jobRoleRepository.count());
    }

    private void seedRole(Map<String, Skill> skillMap, String title, String category,
                          String description, List<String> skillNames) {
        JobRole role = new JobRole();
        role.setTitle(title);
        role.setCategory(category);
        role.setDescription(description);
        Set<Skill> skills = new HashSet<>();
        for (String name : skillNames) {
            skills.add(skillMap.get(name));
        }
        role.setRequiredSkills(skills);
        jobRoleRepository.save(role);
    }

    private Skill ensureSkill(Map<String, Skill> skillMap, String name) {
        Skill existing = skillRepository.findByNameIgnoreCase(name).orElse(null);
        if (existing != null) {
            skillMap.put(name, existing);
            return existing;
        }
        Skill skill = new Skill();
        skill.setName(name);
        skill.setCategory(categoryOf(name));
        skill.setTypicalProficiency(SkillProficiency.INTERMEDIATE);
        return skillRepository.save(skill);
    }

    private String categoryOf(String name) {
        if (List.of("Java", "Spring Boot", "Spring", "Hibernate", "Node.js", "Python",
                "Go", "Rust", "C#", "TypeScript", "JavaScript", "Kotlin", "Swift").contains(name)) {
            return "Programming & Frameworks";
        }
        if (List.of("SQL", "PostgreSQL", "MySQL", "MongoDB", "Redis", "Kafka").contains(name)) {
            return "Database / Messaging";
        }
        if (List.of("Docker", "Kubernetes", "CI/CD", "AWS", "Azure", "GCP", "Linux", "DevOps").contains(name)) {
            return "Infrastructure / DevOps";
        }
        return "Professional Skill";
    }

    private static final List<String> SEED_SKILLS = List.of(
            "Java", "Spring Boot", "Spring", "Hibernate", "REST APIs", "Microservices",
            "SQL", "PostgreSQL", "MongoDB", "Redis", "Kafka", "Docker", "Kubernetes",
            "AWS", "Azure", "GCP", "CI/CD", "Git", "Maven", "Gradle", "JUnit",
            "JavaScript", "TypeScript", "React", "Node.js", "HTML", "CSS",
            "Python", "Pandas", "NumPy", "Machine Learning", "Deep Learning",
            "TensorFlow", "PyTorch", "Scikit-learn", "NLP", "Data Analysis", "Statistics",
            "Spark", "GraphQL", "System Design", "Algorithms", "Data Structures",
            "OOP", "Design Patterns", "Linux", "Shell Scripting", "Networking",
            "Security", "Selenium", "Playwright", "Cypress", "Automation Testing",
            "Communication", "Leadership", "Teamwork", "Problem Solving", "Project Management"
    );
}