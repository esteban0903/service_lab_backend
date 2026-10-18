package com.example.taskassignment.service;

import java.text.Normalizer;
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
}

