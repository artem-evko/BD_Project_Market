package com.shop.system.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class VacationListItemResponse {

    private UUID id;
    private UUID employeeId;
    private String employeeFullName;

    private LocalDate dateStart;
    private LocalDate dateEnd;

    private String status; // pending/approved/rejected
    private String rejectComment;

    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

}
