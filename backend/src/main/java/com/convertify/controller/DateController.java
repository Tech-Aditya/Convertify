package com.convertify.controller;

import com.convertify.config.ApiPaths;
import com.convertify.dto.DateDifferenceRequest;
import com.convertify.dto.DateDifferenceResponse;
import com.convertify.dto.DateOptionsResponse;
import com.convertify.dto.DateShiftRequest;
import com.convertify.dto.DateShiftResponse;
import com.convertify.service.DateCalculatorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Date arithmetic: add/subtract (calendar or business days) and differences between dates. */
@RestController
@RequestMapping(ApiPaths.DATE)
public class DateController {

    private final DateCalculatorService service;

    public DateController(DateCalculatorService service) {
        this.service = service;
    }

    @GetMapping("/options")
    public DateOptionsResponse options() {
        return service.options();
    }

    /** Add or subtract; the direction is the request's {@code operation}. */
    @PostMapping("/shift")
    public DateShiftResponse shift(@Valid @RequestBody DateShiftRequest request) {
        return service.shift(request);
    }

    @PostMapping("/difference")
    public DateDifferenceResponse difference(@Valid @RequestBody DateDifferenceRequest request) {
        return service.difference(request);
    }
}
