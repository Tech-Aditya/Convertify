package com.convertify.controller;

import com.convertify.config.ApiPaths;
import com.convertify.dto.ConversionResponse;
import com.convertify.dto.ConvertAllResponse;
import com.convertify.dto.UnitCatalogResponse;
import com.convertify.dto.UnitConversionRequest;
import com.convertify.dto.UnitConvertAllRequest;
import com.convertify.service.UnitConversionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generic unit conversion for every catalog category. Height, distance and speed are presets
 * over this same endpoint rather than separate APIs.
 */
@RestController
@RequestMapping(ApiPaths.UNITS)
public class UnitController {

    private final UnitConversionService service;

    public UnitController(UnitConversionService service) {
        this.service = service;
    }

    @GetMapping
    public UnitCatalogResponse catalog() {
        return service.catalog();
    }

    @PostMapping("/convert")
    public ConversionResponse convert(@Valid @RequestBody UnitConversionRequest request) {
        return service.convert(request);
    }

    @PostMapping("/convert-all")
    public ConvertAllResponse convertAll(@Valid @RequestBody UnitConvertAllRequest request) {
        return service.convertAll(request);
    }
}
