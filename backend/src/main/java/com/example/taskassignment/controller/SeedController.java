package com.example.taskassignment.controller;

import com.example.taskassignment.dto.SeedSummaryDto;
import com.example.taskassignment.service.SeedService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seed")
public class SeedController {

    private final SeedService seedService;

    public SeedController(SeedService seedService) {
        this.seedService = seedService;
    }

    @PostMapping
    public SeedSummaryDto seed() {
        return seedService.seed();
    }
}
