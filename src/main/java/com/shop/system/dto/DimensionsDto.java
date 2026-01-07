package com.shop.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionsDto {

    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
}
