package com.shop.system.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkReportHeaderListItemResponse {
    private UUID id;
    private LocalDate weekStart;
    private String status; // draft/confirmed/sent
    private LocalDateTime confirmedAt;
    private LocalDateTime sentToHqAt;
    private LocalDateTime createdAt;
}
