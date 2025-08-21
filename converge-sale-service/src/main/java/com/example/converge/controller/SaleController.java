package com.example.converge.controller;

import com.example.converge.dto.request.SaleRequest;
import com.example.converge.dto.response.SaleResponse;
import com.example.converge.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping("/sale")
    public ResponseEntity<SaleResponse> sale(@Valid @RequestBody SaleRequest request) {
        SaleResponse response = saleService.processSale(request);
        return ResponseEntity.ok(response);
    }
}


