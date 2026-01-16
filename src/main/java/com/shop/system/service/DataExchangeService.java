package com.shop.system.service;

import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.ExchangeFileDto;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface DataExchangeService {

    ExchangeFileDto buildReferencePack();

    ApiResponse importReferencePack(MultipartFile file, boolean overwrite);

    ExchangeFileDto exportSales(LocalDate dateFrom, LocalDate dateTo);

    ExchangeFileDto exportStock(LocalDate snapshotDate);

    ExchangeFileDto exportStopList(LocalDate date);

    ExchangeFileDto exportSnapshot(LocalDate snapshotDate);
}
