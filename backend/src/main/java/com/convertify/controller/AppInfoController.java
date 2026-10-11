package com.convertify.controller;

import com.convertify.config.ApiPaths;
import com.convertify.config.AppProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Application name, tagline and version. Also serves as a lightweight health probe. */
@RestController
@RequestMapping(ApiPaths.INFO)
public class AppInfoController {

    private final AppProperties properties;

    public AppInfoController(AppProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public AppProperties info() {
        return properties;
    }
}
