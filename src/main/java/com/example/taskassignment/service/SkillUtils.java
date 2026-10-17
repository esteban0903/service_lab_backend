package com.example.taskassignment.service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class SkillUtils {

    private SkillUtils() {
    }

    /**
     * Normalizes a collection of skills: trims whitespace, converts to lowercase,
     * drops null/blank entries, and deduplicates preserving insertion order.
     */
    public static Set<String> normalizeSkills(Collection<String> skills) {
        if (skills == null) {
            return Collections.emptySet();
        }
        return skills.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}

