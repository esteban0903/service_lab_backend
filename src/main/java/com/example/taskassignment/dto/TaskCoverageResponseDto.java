package com.example.taskassignment.dto;

import java.util.List;

public record TaskCoverageResponseDto(
        int coveredBy,
        List<SkillCoverageItemDto> perSkill,
        List<String> missingSkills,
        String warning
) {}
