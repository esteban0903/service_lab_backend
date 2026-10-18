package com.example.taskassignment.controller;

import com.example.taskassignment.dto.skill.SkillCatalogItemDto;
import com.example.taskassignment.dto.task.TaskCoverageRequestDto;
import com.example.taskassignment.dto.task.TaskCoverageResponseDto;
import com.example.taskassignment.service.SkillService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping("/skills")
    public List<SkillCatalogItemDto> getSkillCatalog() {
        return skillService.getSkillCatalog();
    }

    @PostMapping("/tasks/coverage")
    public TaskCoverageResponseDto getTaskCoverage(@RequestBody(required = false) TaskCoverageRequestDto request) {
        return skillService.evaluateCoverage(request);
    }
}
