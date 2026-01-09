package com.shop.system.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkReportDetailsResponse {

    private UUID id;
    private LocalDate weekStart;

    private String status;
    private LocalDateTime confirmedAt;
    private LocalDateTime sentToHqAt;
    private LocalDateTime createdAt;

    private List<WorkReportLineResponse> lines;
}
