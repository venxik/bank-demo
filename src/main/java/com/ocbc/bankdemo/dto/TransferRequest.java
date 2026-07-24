package com.ocbc.bankdemo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferRequest(
        @NotNull(message = "toAccountId is required")
        Long toAccountId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        BigDecimal amount
) {
}
