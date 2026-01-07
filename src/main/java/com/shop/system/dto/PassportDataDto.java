package com.shop.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
public class PassportDataDto {

    /**
     * internal_rf | international_rf
     */
    private String type;

    // ----- internal_rf -----
    private String series;              // 4 digits
    private String number;              // 6 digits
    private String registrationAddress; // not blank
    private LocalDate issueDate;        // not future

    // ----- international_rf -----
    private String passportNumber;      // 8-10 symbols
    private LocalDate validUntil;       // > issueDate
}
