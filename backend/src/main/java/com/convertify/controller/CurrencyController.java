package com.convertify.controller;

import com.convertify.config.ApiPaths;
import com.convertify.dto.CurrencyConversionRequest;
import com.convertify.dto.CurrencyConversionResponse;
import com.convertify.dto.CurrencyListResponse;
import com.convertify.service.CurrencyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.CURRENCY)
public class CurrencyController {

    private final CurrencyService service;

    public CurrencyController(CurrencyService service) {
        this.service = service;
    }

    @GetMapping("/currencies")
    public CurrencyListResponse currencies() {
        return service.currencies();
    }

    @PostMapping("/convert")
    public CurrencyConversionResponse convert(@Valid @RequestBody CurrencyConversionRequest request) {
        return service.convert(request);
    }
}
