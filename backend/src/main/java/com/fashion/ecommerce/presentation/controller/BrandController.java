package com.fashion.ecommerce.presentation.controller;

import com.fashion.ecommerce.application.service.CatalogService;
import com.fashion.ecommerce.presentation.dto.BrandDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public brand list.
 */
@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final CatalogService catalogService;

    @GetMapping
    public List<BrandDTO> getBrands() {
        return catalogService.getBrands();
    }
}
