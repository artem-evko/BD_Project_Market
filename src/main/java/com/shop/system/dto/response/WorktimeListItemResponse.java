package com.shop.system.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class WorktimeListItemResponse {

    private UUID id;

    private UUID employeeId;
    private String employeeFullName;

    private LocalDate date;

    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;

    private LocalDateTime actualLogin;
    private LocalDateTime actualLogout;

    private String status;

    private Boolean autoClosed;
    private String autoCloseReason;

    private LocalDateTime confirmationSentAt;
    private LocalDateTime confirmationDeadlineAt;
    private String confirmationResult;
    private LocalDateTime confirmationResponseAt;
}
