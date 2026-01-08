package com.shop.system.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CreateVacationRequest {

    @NotNull
    private LocalDate dateStart;

    @NotNull
    private LocalDate dateEnd;

    public LocalDate getDateStart() {
        return dateStart;
    }

    public void setDateStart(LocalDate dateStart) {
        this.dateStart = dateStart;
    }

    public LocalDate getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(LocalDate dateEnd) {
        this.dateEnd = dateEnd;
    }
}
