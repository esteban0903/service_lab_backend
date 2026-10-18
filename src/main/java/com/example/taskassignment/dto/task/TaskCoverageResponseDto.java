package com.example.taskassignment.dto.task;

import com.example.taskassignment.dto.skill.SkillCoverageItemDto;

import java.util.List;

public record TaskCoverageResponseDto(
        int coveredBy,
        List<SkillCoverageItemDto> perSkill,
        List<String> missingSkills,
        String warning
) {}
