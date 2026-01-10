package com.shop.system.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class GenerateScheduleFromTemplateRequest {
    @NotNull
    private LocalDate dateFrom;

    @NotNull
    private LocalDate dateTo;

    /**
     * true = перезаписывать даже если запись уже есть (кроме случаев, когда день уже вручную поправили)
     * false/null = только создавать отсутствующие
     */
    private Boolean overwrite;
}
