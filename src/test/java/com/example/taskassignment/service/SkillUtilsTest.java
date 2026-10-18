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

    @Test
    @DisplayName("hasAllSkills should return true when required skills is empty or null")
    void hasAllSkills_emptyOrNullRequired_shouldReturnTrue() {
        assertThat(SkillUtils.hasAllSkills(List.of("java"), null)).isTrue();
        assertThat(SkillUtils.hasAllSkills(List.of("java"), List.of())).isTrue();
        assertThat(SkillUtils.hasAllSkills(null, null)).isTrue();
    }

    @Test
    @DisplayName("hasAllSkills should return false when worker skills is null or empty and required is not")
    void hasAllSkills_emptyWorkerWithRequired_shouldReturnFalse() {
        assertThat(SkillUtils.hasAllSkills(null, List.of("java"))).isFalse();
        assertThat(SkillUtils.hasAllSkills(List.of(), List.of("java"))).isFalse();
    }

    @Test
    @DisplayName("hasAllSkills should match skills ignoring case, accents, and whitespace")
    void hasAllSkills_normalizationAndSuperset_shouldReturnTrue() {
        List<String> workerSkills = List.of("  JAVA ", "Gestión", "Spring Boot", "Docker");
        List<String> requiredSkills = List.of("java", "gestion");

        assertThat(SkillUtils.hasAllSkills(workerSkills, requiredSkills)).isTrue();
    }

    @Test
    @DisplayName("hasAllSkills should return false when worker lacks at least one required skill")
    void hasAllSkills_missingOne_shouldReturnFalse() {
        List<String> workerSkills = List.of("java", "spring boot");
        List<String> requiredSkills = List.of("java", "docker");

        assertThat(SkillUtils.hasAllSkills(workerSkills, requiredSkills)).isFalse();
    }

    @Test
    @DisplayName("findMissingSkills should return empty list when all skills are present or required is empty")
    void findMissingSkills_allPresentOrEmpty_shouldReturnEmptyList() {
        assertThat(SkillUtils.findMissingSkills(List.of("java", "docker"), List.of("JAVA"))).isEmpty();
        assertThat(SkillUtils.findMissingSkills(List.of("java"), null)).isEmpty();
        assertThat(SkillUtils.findMissingSkills(List.of("java"), List.of())).isEmpty();
    }

    @Test
    @DisplayName("findMissingSkills should return sorted missing skills with normalized names")
    void findMissingSkills_missingSkills_shouldReturnSortedList() {
        List<String> workerSkills = List.of("java");
        List<String> requiredSkills = List.of("Kubernetes", "Docker", "JAVA", "Gestión");

        List<String> missing = SkillUtils.findMissingSkills(workerSkills, requiredSkills);

        assertThat(missing).containsExactly("docker", "gestion", "kubernetes");
    }

    @Test
    @DisplayName("findMissingSkills should return all required skills sorted when worker has none")
    void findMissingSkills_nullWorker_shouldReturnAllSorted() {
        List<String> missing = SkillUtils.findMissingSkills(null, List.of("React", "Angular", "Vue"));

        assertThat(missing).containsExactly("angular", "react", "vue");
    }
}

