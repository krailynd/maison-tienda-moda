package com.fashion.ecommerce.presentation.controller;

import com.fashion.ecommerce.application.service.CatalogService;
import com.fashion.ecommerce.presentation.dto.CategoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public category tree with per-category product counts (catalog sidebar).
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CatalogService catalogService;

    @GetMapping
    public List<CategoryDTO> getCategories() {
        return catalogService.getCategories();
    }
}
