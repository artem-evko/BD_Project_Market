package com.shop.system.service;

import com.shop.system.dto.ManufacturerDto;

import java.util.List;

public interface ManufacturerService {

    List<ManufacturerDto> search(String search, int limit);
}
