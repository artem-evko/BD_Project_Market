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
public class SickLeaveListItemResponse {

    private UUID id;

    private UUID employeeId;
    private String employeeFullName;

    private LocalDate dateStart;
    private LocalDate dateEnd;

    private String status;        // pending/approved/rejected
    private String rejectComment;

    private LocalDateTime createdAt;

    private LocalDateTime approvedAt;
    private String approvedByFullName; // кто подписал/отклонил (если есть)

    private Boolean docRequired;
    private LocalDate docDueDate;
    private LocalDateTime docReceivedAt;

    /**
     * Для UI-фильтра:
     * pending | received | overdue
     * overdue считаем по docDueDate < today и docReceivedAt == null
     */
    private String docUiStatus;
}
