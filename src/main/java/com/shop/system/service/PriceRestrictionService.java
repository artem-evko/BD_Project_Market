package com.shop.system.service;

import com.shop.system.dto.request.CreatePriceRestrictionRequest;
import com.shop.system.dto.request.UpdatePriceRestrictionRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.PriceRestrictionResponse;

import java.util.List;
import java.util.UUID;

public interface PriceRestrictionService {

    List<PriceRestrictionResponse> getForCurrentStoreWithGlobals();

    ApiResponse create(CreatePriceRestrictionRequest request);

    ApiResponse update(UUID id, UpdatePriceRestrictionRequest request);

    ApiResponse delete(UUID id);
}
