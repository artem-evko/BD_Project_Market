package com.shop.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectRequest {

    @NotBlank(message = "rejectComment обязателен")
    private String rejectComment;
}
