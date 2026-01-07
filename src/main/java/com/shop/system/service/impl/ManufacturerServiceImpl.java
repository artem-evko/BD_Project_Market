package com.shop.system.service.impl;

import com.shop.system.domain.entity.Manufacturer;
import com.shop.system.dto.ManufacturerDto;
import com.shop.system.repository.ManufacturerRepository;
import com.shop.system.service.ManufacturerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ManufacturerServiceImpl implements ManufacturerService {

    private final ManufacturerRepository manufacturerRepository;

    @Override
    public List<ManufacturerDto> search(String search, int limit) {
        int size = (limit <= 0 || limit > 50) ? 20 : limit;

        var manufacturers = manufacturerRepository.search(
                search == null ? "" : search.trim(),
                PageRequest.of(0, size)
        );

        return manufacturers.stream()
                .map(this::toDto)
                .toList();
    }

    private ManufacturerDto toDto(Manufacturer m) {
        return ManufacturerDto.builder()
                .id(m.getId())
                .name(m.getManufacturerName())
                .country(m.getManufacturerCountry())
                .build();
    }
}
