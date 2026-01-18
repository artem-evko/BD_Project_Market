package com.shop.system.service;

import com.shop.system.dto.request.ManualPriceRequest;
import com.shop.system.dto.response.CurrentPriceResponse;
import com.shop.system.dto.response.DailySetupResponse;
import com.shop.system.dto.response.ManualPriceResponse;
import com.shop.system.dto.response.PriceHistoryResponse;
import com.shop.system.dto.response.StopListItemResponse;
import com.shop.system.dto.response.StorageLocationResponse;

import java.util.List;
import java.util.UUID;

public interface PriceCalculationService {

    DailySetupResponse dailySetup(UUID employeeId);

    List<CurrentPriceResponse> getCurrentPrices(UUID storageLocationId);

    List<StorageLocationResponse> getStorageLocations();

    List<StopListItemResponse> getStopList();

    List<PriceHistoryResponse> getHistory(UUID productId, Integer days);

    ManualPriceResponse manualChange(ManualPriceRequest request);

}
