package com.shop.system.controller;

import com.shop.system.dto.ManufacturerDto;
import com.shop.system.service.ManufacturerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manufacturers")
public class ManufacturerController {

    private final ManufacturerService manufacturerService;

    @GetMapping
    public List<ManufacturerDto> search(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return manufacturerService.search(search, limit);
    }
}
