package com.example.taskassignment.service;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class SkillUtils {

    private SkillUtils() {
    }

    /**
     * Normalizes a collection of skills: strips accents (NFD + remove combining diacritical marks),
     * collapses repeated inner whitespace, trims, converts to lowercase,
     * drops null/blank entries, and deduplicates preserving insertion order.
     */
    public static Set<String> normalizeSkills(Collection<String> skills) {
        if (skills == null) {
            return Collections.emptySet();
        }
        return skills.stream()
                .filter(Objects::nonNull)
                .map(SkillUtils::normalizeSingleSkill)
                .filter(skill -> !skill.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static String normalizeSingleSkill(String skill) {
        if (skill == null) {
            return "";
        }
        String stripped = Normalizer.normalize(skill.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return stripped.replaceAll("\\s+", " ").toLowerCase().trim();
    }

    /**
     * Checks whether workerSkills satisfies all requiredSkills.
     * Both collections are normalized, and returns true if worker has all required skills
     * (or if requiredSkills is empty/null).
     */
    public static boolean hasAllSkills(Collection<String> workerSkills, Collection<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return true;
        }
        Set<String> normalizedRequired = normalizeSkills(requiredSkills);
        if (normalizedRequired.isEmpty()) {
            return true;
        }
        if (workerSkills == null || workerSkills.isEmpty()) {
            return false;
        }
        Set<String> normalizedWorker = normalizeSkills(workerSkills);
        return normalizedWorker.containsAll(normalizedRequired);
    }

    /**
     * Returns a sorted list of required skills that the worker is missing.
     * Both collections are normalized.
     */
    public static List<String> findMissingSkills(Collection<String> workerSkills, Collection<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> normalizedRequired = normalizeSkills(requiredSkills);
        if (normalizedRequired.isEmpty()) {
            return Collections.emptyList();
        }
        if (workerSkills == null || workerSkills.isEmpty()) {
            return normalizedRequired.stream().sorted().toList();
        }
        Set<String> normalizedWorker = normalizeSkills(workerSkills);
        return normalizedRequired.stream()
                .filter(skill -> !normalizedWorker.contains(skill))
                .sorted()
                .toList();
    }
}

