package com.ocbc.bankdemo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Shared request shape for deposit and withdraw — both are just "move this amount." */
public record AmountRequest(
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        BigDecimal amount
) {
}
