package com.convertify.controller;

import com.convertify.config.ApiPaths;
import com.convertify.dto.TravelCalculationRequest;
import com.convertify.dto.TravelCalculationResponse;
import com.convertify.dto.TravelOptionsResponse;
import com.convertify.service.TravelCalculatorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Distance = Speed × Time calculator. */
@RestController
@RequestMapping(ApiPaths.TIME_DISTANCE)
public class TravelController {

    private final TravelCalculatorService service;

    public TravelController(TravelCalculatorService service) {
        this.service = service;
    }

    @GetMapping("/options")
    public TravelOptionsResponse options() {
        return service.options();
    }

    @PostMapping("/calculate")
    public TravelCalculationResponse calculate(@Valid @RequestBody TravelCalculationRequest request) {
        return service.calculate(request);
    }
}
