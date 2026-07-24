package com.ocbc.bankdemo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank(message = "accountNumber is required")
        String accountNumber,

        @NotBlank(message = "ownerName is required")
        String ownerName,

        @NotNull(message = "initialBalance is required")
        @DecimalMin(value = "0.0", message = "initialBalance cannot be negative")
        BigDecimal initialBalance
) {
}
