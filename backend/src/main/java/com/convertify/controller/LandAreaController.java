package com.convertify.controller;

import com.convertify.config.ApiPaths;
import com.convertify.dto.LandConversionRequest;
import com.convertify.dto.LandConversionResponse;
import com.convertify.dto.LandRegionsResponse;
import com.convertify.service.LandAreaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Land/area conversion with region-dependent traditional units. */
@RestController
@RequestMapping(ApiPaths.AREA)
public class LandAreaController {

    private final LandAreaService service;

    public LandAreaController(LandAreaService service) {
        this.service = service;
    }

    @GetMapping("/regions")
    public LandRegionsResponse regions() {
        return service.regions();
    }

    @PostMapping("/convert")
    public LandConversionResponse convert(@Valid @RequestBody LandConversionRequest request) {
        return service.convert(request);
    }

    @PostMapping("/convert-all")
    public LandConversionResponse convertAll(@Valid @RequestBody LandConversionRequest request) {
        return service.convertAll(request);
    }
}
