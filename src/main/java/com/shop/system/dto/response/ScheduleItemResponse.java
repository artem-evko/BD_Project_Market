package com.shop.system.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ScheduleItemResponse {
    private UUID id;

    private UUID employeeId;
    private String employeeFullName;

    private LocalDate date;
    private LocalTime plannedStart;
    private LocalTime plannedEnd;

    private String scheduleType;

    private String correctionReason;

    private UUID correctedById;
    private String correctedByFullName;

    private UUID templateId;
}
