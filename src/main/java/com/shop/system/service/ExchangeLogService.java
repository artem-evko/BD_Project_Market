package com.shop.system.service;

import com.shop.system.dto.response.ExchangeLogListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ExchangeLogService {

    Page<ExchangeLogListItemResponse> getExchangeLog(
            String entity,
            String direction,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable
    );
}
