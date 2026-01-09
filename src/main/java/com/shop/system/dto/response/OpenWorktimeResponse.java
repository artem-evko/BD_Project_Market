package com.shop.system.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class OpenWorktimeResponse {

    private UUID id;

    private LocalDate date;

    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;

    private LocalDateTime actualLogin;
    private LocalDateTime actualLogout;

    private LocalDateTime confirmationSentAt;
    private LocalDateTime confirmationDeadlineAt;

    private Boolean needsConfirmation; // удобно фронту
}
