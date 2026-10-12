package com.fashion.ecommerce.presentation.controller;

import com.fashion.ecommerce.domain.repository.ColorRepository;
import com.fashion.ecommerce.domain.repository.SeasonRepository;
import com.fashion.ecommerce.domain.repository.SizeRepository;
import com.fashion.ecommerce.presentation.dto.admin.AdminColorRefDTO;
import com.fashion.ecommerce.presentation.dto.admin.AdminSeasonOptionDTO;
import com.fashion.ecommerce.presentation.dto.admin.AdminSizeOptionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only admin reference lookups needed by the admin editors: sizes and
 * colors for the variant editor (slice C), seasons for the collection editor
 * (slice D). This is NOT the slice D category/brand/collection admin —
 * those are full CRUD features handled by their own controllers.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminReferenceController {

    private final SizeRepository sizeRepository;
    private final ColorRepository colorRepository;
    private final SeasonRepository seasonRepository;

    @GetMapping("/sizes")
    public List<AdminSizeOptionDTO> getSizes() {
        return sizeRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(s -> new AdminSizeOptionDTO(s.getId(), s.getName(), s.getCode()))
                .toList();
    }

    @GetMapping("/colors")
    public List<AdminColorRefDTO> getColors() {
        return colorRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(c -> new AdminColorRefDTO(c.getId(), c.getName(), c.getHexCode()))
                .toList();
    }

    @GetMapping("/seasons")
    public List<AdminSeasonOptionDTO> getSeasons() {
        return seasonRepository.findAllByIsActiveTrueOrderByStartDateAsc().stream()
                .map(AdminSeasonOptionDTO::from)
                .toList();
    }
}
