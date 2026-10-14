package com.hiresense.service.ai;

import com.hiresense.service.ai.dto.AiAnswerEvaluation;
import com.hiresense.service.ai.dto.AiLearningPlan;
import com.hiresense.service.ai.dto.AiLearningPlanItem;
import com.hiresense.service.ai.dto.AiQuestion;
import com.hiresense.service.ai.dto.AiResumeAnalysis;
import com.hiresense.service.ai.dto.AiSkillGap;
import com.hiresense.service.ai.dto.AiSkillGapItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic, dependency-free fallback AI provider. Enables the whole
 * platform (resume analysis, skill gaps, interviews, learning plans) to run
 * without any external API key or network access. Behaviour is rule based and
 * consistent, which also keeps the test suite reliable.
 */
@Service
public class MockAiService implements AiService {

    private static final Map<String, String> RESOURCE_URLS = Map.ofEntries(
            Map.entry("java", "https://docs.oracle.com/en/java/"),
            Map.entry("spring boot", "https://docs.spring.io/spring-boot/"),
            Map.entry("spring", "https://spring.io/"),
            Map.entry("react", "https://react.dev/"),
            Map.entry("typescript", "https://www.typescriptlang.org/docs/"),
            Map.entry("javascript", "https://developer.mozilla.org/en-US/docs/Web/JavaScript"),
            Map.entry("python", "https://docs.python.org/3/"),
            Map.entry("docker", "https://docs.docker.com/"),
            Map.entry("kubernetes", "https://kubernetes.io/docs/"),
            Map.entry("postgresql", "https://www.postgresql.org/docs/"),
            Map.entry("aws", "https://aws.amazon.com/training/"),
            Map.entry("algorithms", "https://roadmap.sh/"),
            Map.entry("system design", "https://github.com/donnemartin/system-design-primer")
    );

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public AiResumeAnalysis analyzeResume(String resumeText, List<String> targetSkills) {
        List<String> extracted = extractSkills(resumeText);
        Set<String> extractedLower = lowerSet(extracted);
        int matched = 0;
        for (String target : nullSafe(targetSkills)) {
            if (extractedLower.contains(target.toLowerCase(Locale.ROOT))) {
                matched++;
            }
        }
        int total = Math.max(1, nullSafe(targetSkills).size());
        int keywordMatch = (int) Math.round(matched * 100.0 / total);
        int atsScore = Math.min(96, 45 + keywordMatch / 2 + Math.min(20, extracted.size() * 2));

        List<String> strengths = new ArrayList<>();
        if (!extracted.isEmpty()) {
            strengths.add("Strong coverage of core skills: " + String.join(", ", extracted.stream().limit(5).toList()));
        }
        strengths.add("Clear professional presentation detected in the resume structure");
        if (matched > 0) {
            strengths.add("Matches " + matched + " of " + total + " target skills for the selected role");
        }

        List<String> improvements = new ArrayList<>();
        int missing = total - matched;
        if (missing > 0) {
            improvements.add("Add explicit mentions of the missing target skills to improve keyword matching");
        }
        improvements.add("Quantify achievements with metrics (e.g. percentages, scale, impact)");
        if (resumeText != null && resumeText.length() < 500) {
            improvements.add("Expand on project descriptions to provide more context to recruiters");
        }

        String summary = "The resume shows " + extracted.size() + " identifiable skills with a "
                + keywordMatch + "% keyword match against the target role requirements.";

        return new AiResumeAnalysis(atsScore, keywordMatch, extracted, summary, strengths, improvements);
    }

    @Override
    public List<String> extractSkills(String resumeText) {
        Set<String> found = new LinkedHashSet<>();
        if (resumeText == null || resumeText.isBlank()) {
            return new ArrayList<>();
        }
        String lower = resumeText.toLowerCase(Locale.ROOT);
        for (String skill : SkillDictionary.NAMES) {
            if (lower.contains(skill.toLowerCase(Locale.ROOT))) {
                found.add(skill);
            }
        }
        return new ArrayList<>(found);
    }

    @Override
    public AiSkillGap analyzeSkillGap(List<String> resumeSkills, List<String> requiredSkills, String jobRoleTitle) {
        Set<String> resumeLower = lowerSet(resumeSkills);
        List<AiSkillGapItem> items = new ArrayList<>();
        int matched = 0;
        int total = Math.max(1, nullSafe(requiredSkills).size());

        for (String required : nullSafe(requiredSkills)) {
            boolean has = resumeLower.contains(required.toLowerCase(Locale.ROOT));
            if (has) {
                matched++;
            }
            String currentLevel = has ? "intermediate" : "beginner";
            String gap = has ? "none" : "major";
            String explanation = has
                    ? "This skill appears on your resume and is considered a strength for the role."
                    : "No matching mention found on your resume. Focus on gaining hands-on experience in this area.";
            items.add(new AiSkillGapItem(required, "advanced", currentLevel, gap, explanation));
        }

        int matchScore = (int) Math.round(matched * 100.0 / total);
        String summary = "You match " + matched + " of " + total + " required skills for " + jobRoleTitle
                + " giving a " + matchScore + "% overall match. "
                + (matchScore >= 70 ? "You are well prepared for this role." : "Focus on your biggest gaps below.");

        return new AiSkillGap(matchScore, summary, items);
    }

    @Override
    public List<AiQuestion> generateQuestions(String jobRoleTitle, int count, String difficulty, List<String> focusSkills) {
        List<AiQuestion> questions = new ArrayList<>();
        List<String> skills = nullSafe(focusSkills);
        String skill = skills.isEmpty() ? "core concepts" : skills.get(0);

        String[][] templates = {
                {"TECHNICAL", "Explain {SKILL} and describe a real-world scenario where you have applied it."},
                {"TECHNICAL", "What are the common pitfalls when working with {SKILL}, and how do you avoid them?"},
                {"PROBLEM_SOLVING", "Walk through how you would troubleshoot a complex issue affecting a production system using {SKILL}."},
                {"BEHAVIORAL", "Describe a time you disagreed with a teammate on a technical decision. How did you resolve it?"},
                {"BEHAVIORAL", "Tell me about a project you are proud of and the role you played in its success."},
                {"CODING", "How would you structure a maintainable service that uses {SKILL}? Outline the key components."},
                {"SYSTEM_DESIGN", "Design a high-level architecture for a scalable application. Where does {SKILL} fit in?"},
                {"HR", "Why are you interested in a {ROLE} role specifically, rather than a similar position elsewhere?"},
                {"TECHNICAL", "Discuss the trade-offs you consider when choosing between different approaches related to {SKILL}."},
                {"PROBLEM_SOLVING", "A customer reports an intermittent failure. Describe your systematic approach to diagnosing it."},
                {"TECHNICAL", "Explain a time you had to learn {SKILL} quickly for a deadline. What was your process?"},
                {"BEHAVIORAL", "How do you prioritize competing tasks while working towards a tight deadline?"},
                {"CODING", "Write the steps and considerations for adding {SKILL} to an existing codebase with minimal disruption."},
                {"SYSTEM_DESIGN", "How do you approach reliability, observability and testing when designing a system that depends on {SKILL}?"},
                {"HR", "Where do you see your skills growing over the next two years in this domain?"}
        };

        int n = Math.min(count, 20 - (jobRoleTitle.length() == 0 ? 0 : 0));
        for (int i = 0; i < n && i < templates.length; i++) {
            String[] t = templates[i];
            String question = t[1]
                    .replace("{SKILL}", skill)
                    .replace("{ROLE}", jobRoleTitle);
            List<String> keywords = keywordHints(question, skill);
            questions.add(new AiQuestion(question, t[0], difficultyOr(difficulty, i), keywords));
        }
        // Deduplicate: rotate skills if we run past the template bank
        int idx = 0;
        while (questions.size() < n) {
            String[] t = templates[idx % templates.length];
            String qSkill = skills.isEmpty() ? "core concepts" : skills.get(idx % skills.size());
            String question = t[1].replace("{SKILL}", qSkill).replace("{ROLE}", jobRoleTitle);
            questions.add(new AiQuestion(question, t[0], difficultyOr(difficulty, idx), keywordHints(question, qSkill)));
            idx++;
        }
        return questions;
    }

    @Override
    public AiAnswerEvaluation evaluateAnswer(String jobRoleTitle, String question, String expectedKeywords, String userAnswer) {
        String safeAnswer = userAnswer == null ? "" : userAnswer;
        List<String> expected = splitKeywords(expectedKeywords);
        List<String> missing = new ArrayList<>();
        List<String> matched = new ArrayList<>();
        String lowerAnswer = safeAnswer.toLowerCase(Locale.ROOT);

        for (String kw : expected) {
            String lowerKw = kw.toLowerCase(Locale.ROOT);
            if (lowerAnswer.contains(lowerKw)) {
                matched.add(kw);
            } else {
                missing.add(kw);
            }
        }

        int keywordScore = expected.isEmpty() ? 50 : (int) Math.round(matched.size() * 100.0 / expected.size());
        int depthScore = Math.min(95, 30 + Math.min(65, safeAnswer.length() / 20));
        int clarityScore = computeClarity(safeAnswer);
        int relevanceScore = keywordScore;
        int score = clamp((int) Math.round(keywordScore * 0.5 + depthScore * 0.3 + clarityScore * 0.2));

        String feedback;
        if (score >= 80) {
            feedback = "Excellent answer. You covered the key topics clearly and with good depth.";
        } else if (score >= 60) {
            feedback = "Good answer. You touched on the main ideas but could elaborate further on some points.";
        } else if (score >= 40) {
            feedback = "Acceptable start, but the answer missed several important elements. Review the suggested topics.";
        } else {
            feedback = "The answer needs significant improvement. Refer to the suggested model answer and revise the core concepts.";
        }

        List<String> strengths = new ArrayList<>();
        if (!matched.isEmpty()) {
            strengths.add("Correctly addressed: " + String.join(", ", matched.stream().limit(4).toList()));
        }
        strengths.add("Answered in " + Math.max(1, safeAnswer.split("\\s+").length) + " words");

        List<String> improvements = new ArrayList<>();
        if (!missing.isEmpty()) {
            improvements.add("Missing topics: " + String.join(", ", missing.stream().limit(5).toList()));
        }
        improvements.add("Include concrete examples or metrics to strengthen the response");
        if (safeAnswer.length() < 80) {
            improvements.add("Expand the answer with more detail and structure");
        }

        String suggested = expected.isEmpty()
                ? "Structure your answer as: definition, key points, an example, and a short summary."
                : "A strong answer should cover: " + String.join(", ", expected);

        return new AiAnswerEvaluation(
                score,
                relevanceScore,
                clarityScore,
                depthScore,
                feedback,
                "general",
                strengths,
                improvements,
                missing,
                suggested);
    }

    @Override
    public AiLearningPlan generateLearningPlan(String jobRoleTitle, List<String> weakSkills, String contextSummary) {
        List<String> skills = nullSafe(weakSkills);
        if (skills.isEmpty()) {
            skills = List.of("core domain knowledge");
        }
        List<AiLearningPlanItem> items = new ArrayList<>();
        int order = 1;
        for (String skill : skills) {
            items.add(new AiLearningPlanItem(
                    "Master " + skill,
                    "Build a strong foundation in " + skill + " with hands-on practice and a small project to reinforce learning.",
                    resourceUrlFor(skill),
                    skill,
                    order++,
                    order <= 3 ? 5 : 7));
        }
        items.add(new AiLearningPlanItem(
                "Mock interview practice",
                "Apply the newly learned skills in timed mock interviews and review your evaluations to track improvement.",
                "https://roadmap.sh/",
                "interview preparation",
                order++,
                3));
        items.add(new AiLearningPlanItem(
                "Final skills assessment",
                "Re-run a skill gap analysis to measure progress and identify any remaining weak areas.",
                "https://roadmap.sh/",
                "assessment",
                order++,
                2));

        String description = "A personalized learning path to close the skill gaps identified for " + jobRoleTitle + ". "
                + (contextSummary == null || contextSummary.isBlank()
                ? "Follow the items in order and mark each one complete as you go."
                : contextSummary);

        return new AiLearningPlan(
                "Learning path for " + jobRoleTitle,
                description,
                items);
    }

    // -----------------------------------------------------------------

    private List<String> keywordHints(String question, String skill) {
        List<String> hints = new ArrayList<>();
        for (String word : skill.split("[^A-Za-z0-9.#+]+")) {
            if (word.length() >= 3) {
                hints.add(word);
            }
        }
        if (question.contains("troubleshoot") || question.contains("diagnos")) {
            hints.addAll(List.of("root cause", "monitoring", "logs"));
        }
        if (question.contains("architecture") || question.contains("design")) {
            hints.addAll(List.of("scalability", "availability", "trade-off"));
        }
        if (hints.isEmpty()) {
            hints.add("explain");
        }
        return hints;
    }

    private List<String> splitKeywords(String expectedKeywords) {
        if (expectedKeywords == null || expectedKeywords.isBlank()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (String part : expectedKeywords.split("[,\\n]")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private int computeClarity(String answer) {
        int sentences = Math.max(1, answer.split("[.!?]").length);
        int words = answer.isBlank() ? 1 : answer.split("\\s+").length;
        if (sentences >= 2 && words >= 40) {
            return 90;
        }
        if (words >= 20) {
            return 75;
        }
        return 55;
    }

    private int clamp(int value) {
        return Math.max(5, Math.min(100, value));
    }

    private String difficultyOr(String requested, int index) {
        if (requested != null && !requested.isBlank()) {
            return requested;
        }
        return switch (index % 3) {
            case 0 -> "beginner";
            case 1 -> "intermediate";
            default -> "advanced";
        };
    }

    private Set<String> lowerSet(List<String> values) {
        Set<String> set = new LinkedHashSet<>();
        for (String v : nullSafe(values)) {
            if (v != null) {
                set.add(v.toLowerCase(Locale.ROOT));
            }
        }
        return set;
    }

    private List<String> nullSafe(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String resourceUrlFor(String skill) {
        String key = skill.toLowerCase(Locale.ROOT);
        if (RESOURCE_URLS.containsKey(key)) {
            return RESOURCE_URLS.get(key);
        }
        for (Map.Entry<String, String> entry : RESOURCE_URLS.entrySet()) {
            if (key.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "https://roadmap.sh/";
    }
}