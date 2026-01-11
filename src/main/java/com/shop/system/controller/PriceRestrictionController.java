package com.shop.system.controller;

import com.shop.system.dto.request.CreatePriceRestrictionRequest;
import com.shop.system.dto.request.UpdatePriceRestrictionRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.PriceRestrictionResponse;
import com.shop.system.service.PriceRestrictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/price-restrictions")
public class PriceRestrictionController {

    private final PriceRestrictionService priceRestrictionService;

    /**
     * По ТЗ: директор видит/управляет, остальные могут быть read-only или скрыто.
     * Я оставил GET доступным всем (как read-only), а изменения — только DIRECTOR.
     * Если нужно "скрыть" вообще — добавь @PreAuthorize на GET тоже.
     */
    @GetMapping
    public List<PriceRestrictionResponse> getRules() {
        return priceRestrictionService.getForCurrentStoreWithGlobals();
    }

    @PreAuthorize("hasRole('DIRECTOR')")
    @PostMapping
    public ApiResponse create(@RequestBody CreatePriceRestrictionRequest request) {
        return priceRestrictionService.create(request);
    }

    @PreAuthorize("hasRole('DIRECTOR')")
    @PutMapping("/{id}")
    public ApiResponse update(@PathVariable UUID id, @RequestBody UpdatePriceRestrictionRequest request) {
        return priceRestrictionService.update(id, request);
    }

    @PreAuthorize("hasRole('DIRECTOR')")
    @DeleteMapping("/{id}")
    public ApiResponse delete(@PathVariable UUID id) {
        return priceRestrictionService.delete(id);
    }
}
