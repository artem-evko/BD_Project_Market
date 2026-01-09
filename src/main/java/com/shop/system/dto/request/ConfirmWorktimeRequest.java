package com.shop.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmWorktimeRequest {

    /**
     * "continue" или "stop"
     */
    @NotBlank
    private String result;
}
