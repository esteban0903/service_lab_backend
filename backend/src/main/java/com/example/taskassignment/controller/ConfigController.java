package com.example.taskassignment.controller;

import com.example.taskassignment.dto.AppConfigDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final boolean seedEnabled;

    public ConfigController(@Value("${app.seed.enabled:false}") boolean seedEnabled) {
        this.seedEnabled = seedEnabled;
    }

    @GetMapping
    public AppConfigDto getConfig() {
        return new AppConfigDto(seedEnabled);
    }
}

