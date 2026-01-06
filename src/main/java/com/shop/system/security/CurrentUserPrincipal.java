package com.shop.system.security;

import java.io.Serializable;
import java.util.UUID;

public record CurrentUserPrincipal(
        UUID employeeId,
        String login,
        String role
) implements Serializable {
}
