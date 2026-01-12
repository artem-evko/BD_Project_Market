package com.shop.system.service.impl;

import com.shop.system.domain.entity.ExchangeLog;
import com.shop.system.dto.response.ExchangeLogListItemResponse;
import com.shop.system.exception.BusinessException;
import com.shop.system.repository.ExchangeLogRepository;
import com.shop.system.service.ExchangeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@Service
@RequiredArgsConstructor
public class ExchangeLogServiceImpl implements ExchangeLogService {

    private final ExchangeLogRepository exchangeLogRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ExchangeLogListItemResponse> getExchangeLog(
            String entity,
            String direction,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable
    ) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BusinessException("dateFrom не может быть больше dateTo");
        }

        entity = normalize(entity);
        direction = normalize(direction);
        status = normalize(status);

        ZoneId zone = ZoneId.systemDefault();

        Instant fromInstant = (dateFrom == null)
                ? null
                : dateFrom.atStartOfDay(zone).toInstant();

        Instant toInstant = (dateTo == null)
                ? null
                : dateTo.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1);

        return exchangeLogRepository.search(entity, direction, status, fromInstant, toInstant, pageable)
                .map(this::toResponse);
    }

    private String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private ExchangeLogListItemResponse toResponse(ExchangeLog e) {
        return ExchangeLogListItemResponse.builder()
                .id(e.getId())
                .timestamp(e.getTimestamp())
                .entityName(e.getEntityName())
                .entityId(e.getEntityId())
                .operation(e.getOperation())
                .direction(e.getDirection())
                .status(e.getStatus())
                .message(e.getMessage())
                .build();
    }
}