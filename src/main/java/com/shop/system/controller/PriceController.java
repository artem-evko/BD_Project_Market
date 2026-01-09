package com.shop.system.controller;

import com.shop.system.dto.request.ManualPriceRequest;
import com.shop.system.dto.response.CurrentPriceResponse;
import com.shop.system.dto.response.DailySetupResponse;
import com.shop.system.dto.response.ManualPriceResponse;
import com.shop.system.dto.response.PriceHistoryResponse;
import com.shop.system.dto.response.StopListItemResponse;
import com.shop.system.security.CurrentUserPrincipal;
import com.shop.system.service.PriceCalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
public class PriceController {

    private final PriceCalculationService priceCalculationService;

    @PostMapping("/daily-setup")
    public DailySetupResponse dailySetup() {
        return priceCalculationService.dailySetup(currentEmployeeId());
    }

    @GetMapping("/current")
    public List<CurrentPriceResponse> currentPrices(@RequestParam UUID storageLocationId) {
        return priceCalculationService.getCurrentPrices(storageLocationId);
    }

    @GetMapping("/stop-list")
    public List<StopListItemResponse> stopList() {
        return priceCalculationService.getStopList();
    }

    @GetMapping("/history/{productId}")
    public List<PriceHistoryResponse> history(@PathVariable UUID productId,
                                              @RequestParam(defaultValue = "30") Integer days) {
        return priceCalculationService.getHistory(productId, days);
    }

    @PostMapping("/manual")
    public ManualPriceResponse manual(@RequestBody @Valid ManualPriceRequest request) {
        return priceCalculationService.manualChange(request);
    }

    private UUID currentEmployeeId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CurrentUserPrincipal p) {
            return p.employeeId();
        }
        throw new IllegalStateException("Current user principal is not CurrentUserPrincipal");
    }
}