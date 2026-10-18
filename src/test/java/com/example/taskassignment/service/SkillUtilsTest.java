package com.example.taskassignment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SkillUtilsTest {

    @Test
    @DisplayName("Should normalize skills: trim, lowercase, drop blanks and nulls, and deduplicate")
    void shouldNormalizeSkillsCorrectly() {
        List<String> rawSkills = Arrays.asList(
                "  Java ",
                "JAVA",
                "  ",
                null,
                "Spring Boot",
                "spring boot",
                "Docker"
        );

        Set<String> normalized = SkillUtils.normalizeSkills(rawSkills);

        assertThat(normalized)
                .containsExactly("java", "spring boot", "docker");
    }

    @Test
    @DisplayName("Should remove accents and diacritics via NFD decomposition")
    void shouldRemoveAccentsAndDiacritics() {
        List<String> rawSkills = List.of(
                "Gestión",
                "Café Con Leche",
                "Programación",
                "C# Básico",
                "Node.JS Avanzado"
        );

        Set<String> normalized = SkillUtils.normalizeSkills(rawSkills);

        assertThat(normalized)
                .containsExactly("gestion", "cafe con leche", "programacion", "c# basico", "node.js avanzado");
    }

    @Test
    @DisplayName("Should collapse repeated inner whitespace")
    void shouldCollapseRepeatedInnerWhitespace() {
        List<String> rawSkills = List.of(
                "Spring    Boot",
                "  Data \t\t Science  \n Machine   Learning  "
        );

        Set<String> normalized = SkillUtils.normalizeSkills(rawSkills);

        assertThat(normalized)
                .containsExactly("spring boot", "data science machine learning");
    }

    @Test
    @DisplayName("Should return empty set when skills list is null or empty")
    void shouldReturnEmptySetForNullOrEmpty() {
        assertThat(SkillUtils.normalizeSkills(null)).isEmpty();
        assertThat(SkillUtils.normalizeSkills(List.of())).isEmpty();
    }
}

