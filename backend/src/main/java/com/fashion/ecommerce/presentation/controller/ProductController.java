package com.fashion.ecommerce.presentation.controller;

import com.fashion.ecommerce.application.service.CatalogService;
import com.fashion.ecommerce.presentation.dto.PageResponse;
import com.fashion.ecommerce.presentation.dto.ProductDetailDTO;
import com.fashion.ecommerce.presentation.dto.ProductSummaryDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public product catalog endpoints.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final CatalogService catalogService;

    @GetMapping
    public PageResponse<ProductSummaryDTO> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletResponse response) {
        PageResponse<ProductSummaryDTO> result =
                catalogService.getProducts(category, search, sort, page, size);
        response.setHeader("X-Total-Count", String.valueOf(result.totalElements()));
        return result;
    }

    @GetMapping("/{slugOrId}")
    public ProductDetailDTO getProduct(@PathVariable String slugOrId) {
        return catalogService.getProduct(slugOrId);
    }

    @GetMapping("/{slugOrId}/related")
    public List<ProductSummaryDTO> getRelated(@PathVariable String slugOrId) {
        return catalogService.getRelated(slugOrId);
    }
}
