package com.shop.system.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateInventoryRequest {
    @NotNull
    private LocalDate inventoryDate;
}