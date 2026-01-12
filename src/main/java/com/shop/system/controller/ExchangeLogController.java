package com.shop.system.controller;

import com.shop.system.dto.response.ExchangeLogListItemResponse;
import com.shop.system.service.ExchangeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exchange-log")
public class ExchangeLogController {

    private final ExchangeLogService exchangeLogService;

    /**
     * GET /api/exchange-log?entity=&direction=&status=&dateFrom=&dateTo=
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR')")
    public Page<ExchangeLogListItemResponse> getExchangeLog(
            @RequestParam(name = "entity", required = false) String entity,
            @RequestParam(name = "direction", required = false) String direction,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @PageableDefault(sort = "timestamp", direction = DESC) Pageable pageable
    ) {
        return exchangeLogService.getExchangeLog(entity, direction, status, dateFrom, dateTo, pageable);
    }
}